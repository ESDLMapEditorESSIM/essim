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
package nl.tno.essim.observation.consumers;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NATSObservationConsumer implements IObservationConsumer {

    private static final String OBSERVATION_TOPIC = "observation";
    private long timestamp;
    private String observationTopic;
    private Connection nc;
    private String url;
    private JetStream jetStream;

    public NATSObservationConsumer(String url) {
        this.url = url;
    }

    @Override
    public void init(String scenarioName) {
        observationTopic = scenarioName + "." + OBSERVATION_TOPIC;
        try {
            Options options = Options.builder().server(url).maxMessagesInOutgoingQueue(100000).useDispatcherWithExecutor().build();
            nc = Nats.connect(options);
            configureJetStream();
        } catch (IOException | JetStreamApiException | InterruptedException e) {
//        } catch (IOException | InterruptedException e) {
            log.warn("Could not create nats connection", e);
        }
    }

    private void configureJetStream() throws IOException, JetStreamApiException {
        jetStream = nc.jetStream();
        JetStreamManagement jsm = nc.jetStreamManagement();
        String streamName = "EssimObservations";
        StreamConfiguration sc = StreamConfiguration.builder()
                .name(streamName)
                .storageType(StorageType.File)
                .subjects("*" + "." + OBSERVATION_TOPIC)
                .build();
        StreamInfo si = jsm.addStream(sc);
        log.info("Created stream {} with subject(s) {}", streamName, si.getConfiguration().getSubjects());
    }

    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {

        if (source.getProviderName()
                .equals("ESSIM")) {
            return;
        }

        JSONObject point = new JSONObject();

        // Measurement & Time
        point.put("measurement", source.getProviderName());
        timestamp = observation.getObservedAt()
                .atZone(EssimTime.defaultTimeZone)
                .toEpochSecond();
        point.put("time", timestamp);

        // Fields
        JSONObject fieldSet = new JSONObject();
        for (Entry<String, Object> field : observation.getValues()
                .entrySet()) {
            fieldSet.put(field.getKey(), field.getValue());
        }
        point.put("fields", fieldSet);

        // Tags
        JSONObject tagSet = new JSONObject();
        for (Entry<String, String> tag : observation.getTags()
                .entrySet()) {
            tagSet.put(tag.getKey(), tag.getValue());
        }
        tagSet.put("simulationRun", simulationRunName);
        point.put("tags", tagSet);

        if (nc != null) {
            try {
                jetStream.publish(observationTopic, point.toString().getBytes("UTF-8"));
            } catch (IOException | JetStreamApiException e) {
                log.warn("Could not publish nats message", e);
            }
        }
    }

    @Override
    public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanup() {
        JSONObject endOfSimulation = new JSONObject();
        endOfSimulation.put("event", "endOfSimulation");
        endOfSimulation.put("lastTimestamp", timestamp);
        try {
            nc.publish(observationTopic, endOfSimulation.toString()
                    .getBytes());
            nc.flush(null);
            nc.close();
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

    }

}
