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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.EnergyAsset;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@Slf4j
public class RemoteLogicNode extends Node {

	private final NodeConfiguration remoteLogicConfig;
	private final Map<Long, Object> locks;
	private final MqttClient client;
	private final String remoteId;

	RemoteLogicNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray, JSONObject geoJSON,
			EnergyAsset asset, int directionFactor, Role role, TreeMap<Double, Double> demandFunction, double energy,
			double cost, Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now,
			NodeConfiguration config) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role, demandFunction, energy,
				cost, parent, carrier, children, timeStep, now);
		this.locks = new HashMap<>();
		this.remoteLogicConfig = config;
		this.remoteId = (asset == null || asset.getName() == null) ? nodeId : asset.getName();

		String serverURI = "tcp://" + config.getMqttHost() + ":" + config.getMqttPort();
		try {
			UUID randomId = UUID.randomUUID();
			this.client = new MqttClient(serverURI, randomId.toString());

			/*
			 * client.setCallback(new MqttCallback() {
			 * 
			 * @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
			 * log.info("Also here received new message at topic: {}", topic); }
			 * 
			 * @Override public void deliveryComplete(IMqttDeliveryToken token) { log.info("Delivery complete"); }
			 * 
			 * @Override public void connectionLost(Throwable cause) { log.error("Connection lost");
			 * 
			 * } });
			 */
			this.client.connect();
			this.client.subscribe(config.getMqttTopic() + "/simulation/" + remoteId + "/#", this::receiveMessage);
			this.publishConfig();
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	private void publishConfig() {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			serializeESDLNode(bos);
			MqttMessage msg = new MqttMessage(bos.toByteArray());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + this.remoteId + "/config", msg);
		} catch (MqttException | IOException e) {
			// FIXME this is now the default behavior
			log.warn("Unable to send node asset info");
		}
	}

	private void serializeESDLNode(ByteArrayOutputStream bos) throws IOException {
		XMIResource xmiResource = new XMIResourceImpl();
		xmiResource.getContents()
				.add(asset);
		xmiResource.save(bos, new HashMap<String, Object>());
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		super.createBidCurve(timeStep, now, minPrice, maxPrice);

		long t = now.getStartTime()
				.toEpochSecond(ZoneOffset.UTC);
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putLong(t);
		buf.putLong(now.getPeriod()
				.getSeconds());
		buf.putDouble(minPrice);
		buf.putDouble(maxPrice);

		try {
			MqttMessage msg = new MqttMessage(buf.array());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + this.nodeId + "/createBid", msg);
		} catch (MqttException e) {
			e.printStackTrace();
		}

		this.locks.put(t, new Object());
		synchronized (this.locks.get(t)) {
			try {
				this.locks.get(t)
						.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		super.processAllocation(timestamp, builder, price);
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putLong(timestamp.getTime()
				.toEpochSecond(ZoneOffset.UTC));
		buf.putDouble(price);

		try {
			MqttMessage msg = new MqttMessage(buf.array());
			this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + remoteId + "/allocate", msg);
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
					this.demandFunction = new TreeMap<>();

					while (buf.remaining() >= 16) {
						this.demandFunction.put(buf.getDouble(), buf.getDouble());
					}

					// Resume the createBidCurve thread
					synchronized (this.locks.get(timestep)) {
						this.locks.get(timestep)
								.notify();
					}

				}
			}
		} catch (Exception e) {
			log.warn("Error handling MQTT message: {}", e.getMessage());
			log.trace(e.getMessage(), e);
		}
	}

}
