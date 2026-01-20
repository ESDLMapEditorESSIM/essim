/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */

package nl.tno.essim.transportsolver.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.Port;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.MarkerFactory;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class RemoteLogicNode extends Node {

    private final NodeConfiguration remoteLogicConfig;
    private final Map<Long, Object> locks;
    private final MqttClient client;
    private final String esdlString;
    private JsonNode remoteConfig;
    private String carrierId;
    private static final String MQTT_USERNAME = "essim-mso";
    private static final String MQTT_PASSWORD = "Who Does Not Like Essim!?";

    RemoteLogicNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                    String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                    Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, NodeConfiguration config,
                    Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        this.esdlString = esdlString;
        this.locks = new HashMap<>();
        this.remoteLogicConfig = config;
        carrierId = carrier.getId();
        @SuppressWarnings("unchecked")
        HashMap<String, ?> remoteConfigMap = (HashMap<String, ?>) config.getConfig();
        remoteConfig = ESSimEngine.getObjectMapper().valueToTree(remoteConfigMap);

        log.debug(MarkerFactory.getMarker(simulationId), "Creating Remote Logic Node for asset : {} ({})", asset.getId(),
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
            this.client.subscribe(config.getMqttTopic() + "/simulation/" + this.nodeId + "/" + this.carrierId + "/#",
                    this::receiveMessage);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void publishConfig() {
        try {
            ObjectNode node = ESSimEngine.getObjectMapper().createObjectNode();
            node.put("esdlContents", Base64.getEncoder().encodeToString(esdlString.getBytes()));
            node.put("simulationId", simulationId);
            node.set("config", remoteConfig);
            MqttMessage msg = new MqttMessage(ESSimEngine.getObjectMapper().writeValueAsBytes(node));
            this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/config", msg);
        } catch (MqttException | JsonProcessingException e) {
            // FIXME this is now the default behavior
            log.warn(MarkerFactory.getMarker(simulationId), "Unable to send node asset info", e);
        }
    }

    @Override
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        updateMarketInfo(marketInfo);

        long t = now.getStartTime().toEpochSecond(ZoneOffset.UTC);

        ObjectNode message = ESSimEngine.getObjectMapper().createObjectNode().put("timeStamp", t).put("timeStepInSeconds", now.getPeriod().getSeconds())
                .put("minPrice", marketInfo.getMinPrice().doubleValue()) // TODO: check if this is the correct place to
                // communicate min/max price
                .put("maxPrice", marketInfo.getMaxPrice().doubleValue())
                .put("deltaPrice", marketInfo.getDeltaPrice().doubleValue()).put("carrierId", carrier.getId());
        try {
            MqttMessage msg = new MqttMessage(ESSimEngine.getObjectMapper().writeValueAsBytes(message));
            this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/createBid", msg);
        } catch (MqttException | JsonProcessingException e) {
            log.warn(MarkerFactory.getMarker(simulationId), "Unable to send node message", e);
        }

        /**
         * wait for remote node to respond with the bidcurve (@see
         * #receiveMessage(String topic, MqttMessage message))
         **/
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
    public void calculateAssignedEnergies(EssimTime timestamp, BigDecimal price) {
        super.calculateAssignedEnergies(timestamp, price);
        ObjectNode message = ESSimEngine.getObjectMapper().createObjectNode().put("timeStamp", timestamp.getTime().toEpochSecond(ZoneOffset.UTC))
                .put("price", price).put("carrierId", carrier.getId());

        try {
            MqttMessage msg = new MqttMessage(ESSimEngine.getObjectMapper().writeValueAsBytes(message));
            this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/allocate", msg);
        } catch (MqttException | JsonProcessingException e) {
            log.warn(MarkerFactory.getMarker(simulationId), "Unable to send node message", e);
        }
    }


    @Override
    public void processAllocation(EssimTime timestamp, ObservationBuilder observation) {
        if (role.equals(Role.CONSUMER)) {
            EmissionManager.getInstance(simulationId).addConsumer(networkId, asset, Math.abs(energy));
        } else {
            EmissionManager.getInstance(simulationId).addProducer(networkId, asset, Math.abs(energy));
        }
    }

    public void stop() {
        ObjectNode message = ESSimEngine.getObjectMapper().createObjectNode().put("carrierId", carrier.getId());

        try {
            MqttMessage msg = new MqttMessage(ESSimEngine.getObjectMapper().writeValueAsBytes(message));
            this.client.publish(this.remoteLogicConfig.getMqttTopic() + "/node/" + nodeId + "/stop", msg);
        } catch (MqttException | JsonProcessingException e) {
            log.warn(MarkerFactory.getMarker(simulationId), "Unable to send node message", e);
        }
    }

    private void receiveMessage(String topic, MqttMessage message) {
        log.trace(MarkerFactory.getMarker(simulationId), "Received new message at topic: {}", topic);
        try {
            if (topic.endsWith("bid")) {
                ByteBuffer buf = ByteBuffer.wrap(message.getPayload());
                long timestep = buf.getLong();

                if (this.locks.containsKey(timestep)) {
                    ArrayList<double[]> list = new ArrayList<double[]>();
                    String point = "";
                    while (buf.remaining() >= 16) {
                        double price = buf.getDouble();
                        double energy = buf.getDouble();
                        point += "[" + price + "," + energy + "],";
                        list.add(new double[]{price, energy});
                    }
                    this.demandFunction = new BidFunction(getMinMarketPrice(), getMaxMarketPrice(), getDeltaPrice(),
                            list.toArray(new double[][]{}));
                    log.trace(MarkerFactory.getMarker(simulationId), "Received for timestep " + timestep + ", carrier " + carrierId + ", bid curve is "
                            + point.substring(0, point.length() - 1));

                    // Resume the createBidCurve thread
                    synchronized (this.locks.get(timestep)) {
                        this.locks.get(timestep).notify();
                    }

                }
            }
        } catch (Exception e) {
            log.warn(MarkerFactory.getMarker(simulationId), "Error handling MQTT message: {}", e.getMessage(), e);
        }
    }

}
