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

import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;

import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

@Slf4j
public class KafkaObservationConsumer implements IObservationConsumer {

    private final static int LINGER_MS = (int) Duration.ofSeconds(1).toMillis(); // Wait 1 seconds or until 500 records are gathered,
    private final static int BATCH_SIZE = 2000; // whichever is first, before publishing
    private final static int PARTITIONS = 3;
    private KafkaProducer<String, String> producer;
    private static final String OBSERVATION_TOPIC = "observation";
    private String topicPrefix;
    private long timestamp;
    private String observationTopic;
    private String url;

    public KafkaObservationConsumer(String url) {
        this.url = url;
    }

    @Override
    public void init(String simulationId) {
        this.topicPrefix = simulationId;
        observationTopic = topicPrefix + "." + OBSERVATION_TOPIC;

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "0");
        properties.put(ProducerConfig.RETRIES_CONFIG, 10);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-" + KafkaObservationConsumer.class.getSimpleName());
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        producer = new KafkaProducer<String, String>(properties);
    }

    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
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
        tagSet.put("simulationId", topicPrefix);
        point.put("tags", tagSet);

        if (producer != null) {
            producer.send(new ProducerRecord<String, String>(observationTopic, observationTopic, point.toString()));
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
        producer.send(
                new ProducerRecord<String, String>(observationTopic, observationTopic, endOfSimulation.toString()));
        producer.flush();
        producer.close();
    }
}
