package nl.tno.essim.kpi;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;
import nl.tno.essim.kpi.kafka.JacksonDeserializer;
import nl.tno.essim.kpi.kafka.JacksonSerializer;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public class KafkaKPICommunicator extends KPICommunicator {
    private final static Duration POLL_DURATION = Duration.ofSeconds(1);
    private final static int LINGER_MS = (int) Duration.ofSeconds(1).toMillis(); // Wait 1 seconds or until 2000 records are gathered,
    private final static int BATCH_SIZE = 2000; // whichever is first, before publishing
    private final static int PARTITIONS = 1;

    private final static String DATA_TOPIC_NAME = OBSERVATION_PREFIX + "all";

    private final String url;
    private KafkaProducer<String, KPIData> dataProducer;
    private KafkaProducer<String, KPIControlMessage> controlProducer;

    public KafkaKPICommunicator(KPIManager kpiManager, String url) {
        super(kpiManager);
        this.url = url;
        init();
    }

    private void init() {
        createDataProducer();
        createControlProducer();
        KafkaConsumer<String, KPIModuleMessage> consumer = createResponseMessageKafkaConsumer();
        List<String> topicNames = Collections.singletonList(KPI_RESPONSE_TOPIC);
        consumer.subscribe(topicNames);
        log.info("Initialized kafka consumer for {}, group name {}, subscribed to {}", KafkaKPICommunicator.class.getCanonicalName(), consumer.groupMetadata().groupId(), topicNames);
        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, KPIModuleMessage> records = consumer.poll(POLL_DURATION);
                records.forEach(r -> responseReceived(r.value()));
            }
        }, "KafkaResponseConsumer").start();

    }

    private KafkaConsumer<String, KPIModuleMessage> createResponseMessageKafkaConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        String groupName = KafkaKPICommunicator.class.getSimpleName() + "Group";
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, KafkaKPICommunicator.class.getSimpleName());
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000); // Default 500
        return new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JacksonDeserializer<>(objectMapper, KPIModuleMessage.class));
    }

    private void createControlProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
//        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "0");
        properties.put(ProducerConfig.RETRIES_CONFIG, 10);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-Engine");
        controlProducer = new KafkaProducer<>(properties);
        createTopic(properties, KPI_CONTROL_TOPIC);
    }

    private void createDataProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "0");
        properties.put(ProducerConfig.RETRIES_CONFIG, 10);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-" + KafkaKPICommunicator.class.getSimpleName());
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        dataProducer = new KafkaProducer<>(properties);
        createTopic(properties, DATA_TOPIC_NAME);
    }

    @Override
    public void sendData(KPIData data) {
        String simulationId = data.getSimulationId();
//        log.debug("Sending message on topic {}", DATA_TOPIC_NAME);
        dataProducer.send(new ProducerRecord<>(DATA_TOPIC_NAME, simulationId, data));
    }

    @Override
    protected void sendControlMessage(KPIControlMessage message) {
        log.trace("Sending control message: {}", message);
        controlProducer.send(new ProducerRecord<>(KPI_CONTROL_TOPIC, message));
    }

    @Override
    public Type getType() {
        return Type.KAFKA;
    }

    private static void createTopic(Properties props, String topic) {
        Properties adminProperties = new Properties();
        adminProperties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        adminProperties.setProperty(AdminClientConfig.CLIENT_ID_CONFIG, props.getProperty(ProducerConfig.CLIENT_ID_CONFIG));

        try {
            AdminClient kafkaAdminClient = KafkaAdminClient.create(adminProperties);
            Set<String> existingTopics = kafkaAdminClient.listTopics().names().get();
            if (!existingTopics.contains(topic)) {
                log.info("Creating topic: {}", topic);
                CreateTopicsResult result = kafkaAdminClient.createTopics(Collections.singletonList(new NewTopic(topic, PARTITIONS, (short) 1)));
                result.all().get();
            }
            kafkaAdminClient.close();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error creating kafka topic {}", topic, e);
        }
    }

}
