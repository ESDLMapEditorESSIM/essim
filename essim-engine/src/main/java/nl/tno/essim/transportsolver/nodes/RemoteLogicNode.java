/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.transportsolver.nodes;

import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.Port;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@Slf4j
public class RemoteLogicNode extends Node {

	private final NodeConfiguration remoteLogicConfig;
	private final Map<Long, Object> locks;
	private final MqttClient client;
	private JSONObject remoteConfig;
	private static final String MQTT_USERNAME = "essim-mso";
	private static final String MQTT_PASSWORD = "Who Does Not Like Essim!?";

	RemoteLogicNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
			Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, NodeConfiguration config,
			Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
				energy, cost, parent, carrier, children, timeStep, now, connectedPort);
		this.locks = new HashMap<>();
		this.remoteLogicConfig = config;
		@SuppressWarnings("unchecked")
		HashMap<String, ?> remoteConfigMap = (HashMap<String, ?>) config.getConfig();
		remoteConfig = new JSONObject(remoteConfigMap);

		log.debug("Creating Remote Logic Node for asset : {} ({})", asset.getId(),
				asset.getClass().getInterfaces()[0].getSimpleName());

		String serverURI = "tcp://" + config.getMqttHost() + ":" + config.getMqttPort();
		try {
			UUID randomId = UUID.randomUUID();
			this.client = new MqttClient(serverURI, randomId.toString());

			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setMaxInflight(50000);
			connOpts.setCleanSession(true);

			connOpts.setUserName(MQTT_USERNAME);
			connOpts.setPassword(MQTT_PASSWORD.toCharArray());
			this.client.connect(connOpts);
			this.client.subscribe(config.getMqttTopic() + "/simulation/" + this.nodeId + "/#", this::receiveMessage);
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private void publishConfig() {
		try {
			JSONObject message = new JSONObject()
					.put("esdlContents", Base64.getEncoder().encodeToString(esdlString.getBytes()))
					.put("simulationId", simulationId)
					.put("config", remoteConfig);
			MqttMessage msg = new MqttMessage(message.toString().getBytes());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/config", msg);
		} catch (MqttException e) {
			// FIXME this is now the default behavior
			log.warn("Unable to send node asset info");
			e.printStackTrace();
		}
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		super.createBidCurve(timeStep, now, minPrice, maxPrice);

		long t = now.getStartTime().toEpochSecond(ZoneOffset.UTC);

		JSONObject message = new JSONObject().put("timeStamp", t)
				.put("timeStepInSeconds", now.getPeriod().getSeconds())
				.put("minPrice", minPrice)
				.put("maxPrice", maxPrice)
				.put("carrierId", carrier.getId());
		try {
			MqttMessage msg = new MqttMessage(message.toString().getBytes());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/createBid", msg);
		} catch (MqttException e) {
			e.printStackTrace();
		}

		this.locks.put(t, new Object());
		synchronized (this.locks.get(t)) {
			try {
				this.locks.get(t).wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void init() {
		publishConfig();
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		JSONObject message = new JSONObject().put("timeStamp", timestamp.getTime().toEpochSecond(ZoneOffset.UTC))
				.put("price", price)
				.put("carrierId", carrier.getId());

		try {
			MqttMessage msg = new MqttMessage(message.toString().getBytes());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/allocate", msg);
		} catch (MqttException e) {
			e.printStackTrace();
		}

		if (role.equals(Role.CONSUMER)) {
			EmissionManager.getInstance(simulationId).addConsumer(networkId, asset, Math.abs(energy));
		} else {
			EmissionManager.getInstance(simulationId).addProducer(networkId, asset, Math.abs(energy));
		}
	}

	public void stop() {
		JSONObject message = new JSONObject().put("carrierId", carrier.getId());

		try {
			MqttMessage msg = new MqttMessage(message.toString().getBytes());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/stop", msg);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	private void receiveMessage(String topic, MqttMessage message) {
		log.trace("Received new message at topic: {}", topic);
		try {
			if (topic.endsWith("bid")) {
				ByteBuffer buf = ByteBuffer.wrap(message.getPayload());
				long timestep = buf.getLong();

				if (this.locks.containsKey(timestep)) {
					this.demandFunction = new BidFunction();

					while (buf.remaining() >= 16) {
						this.demandFunction.addPoint(buf.getDouble(), buf.getDouble());
					}

					// Resume the createBidCurve thread
					synchronized (this.locks.get(timestep)) {
						this.locks.get(timestep).notify();
					}

				}
			}
		} catch (Exception e) {
			log.warn("Error handling MQTT message: {}", e.getMessage());
			log.trace(e.getMessage(), e);
		}
	}

}
