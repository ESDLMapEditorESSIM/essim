package nl.tno.essim.kpi;

import com.github.jcustenborder.kafka.serialization.jackson.JacksonDeserializer;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;
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
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
public class KafkaKPIModule extends KPIModule {
    private static final String DATA_TOPIC = "observation.all";
    private final static Duration POLL_DURATION = Duration.ofSeconds(1);
    private final static Properties consumerProperties;
    private final static int PARTITIONS = 1;
    private KafkaProducer<String, KPIModuleMessage> producer;
    private static String url;
    static {
        url = System.getProperty("KAFKA_URL");
        if (url == null) {
            url = "localhost:9092";
        }
        consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        String groupName = KPIModule.class.getSimpleName() + "Group";
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, KPIModule.class.getSimpleName());
        consumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000); // Default 500
    }

    public KafkaKPIModule(KPIProcessorFactory kpiProcessorFactory) {
        super(kpiProcessorFactory);
    }

    public void start() {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "0");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-" + kpiProcessorFactory.getModuleInfo().getTypeId());
        Map<String, String> mapperSettings = JacksonSerializer.nonDefaultSettings(objectMapper);
        JacksonSerializer<KPIModuleMessage> serializer = new JacksonSerializer<>();
        serializer.configure(mapperSettings, true);
        producer = new KafkaProducer<>(producerProperties, new StringSerializer(), serializer);

        KafkaConsumer<String, KPIControlMessage> controlConsumer = createControlConsumer();
        KafkaConsumer<String, KPIData> dataConsumer = createDataConsumer();
        sendStatus();
        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, KPIData> records = dataConsumer.poll(POLL_DURATION);
                log.trace("Received {} data records", records.count());
                records.forEach(r -> handleDataMessage(r.key(), r.value()));
            }
        }, "ObservationConsumer").start();
        while (true) {
            ConsumerRecords<String, KPIControlMessage> records = controlConsumer.poll(POLL_DURATION);
            records.forEach(r -> handleKPIControlMessage(r.value()));
        }
    }

    @Override
    protected void sendMessage(KPIModuleMessage m) {
        producer.send(new ProducerRecord<>(KPI_RESPONSE_TOPIC, uuid, m));
    }

    @Override
    protected void sendStatus() {
        super.sendStatus();
    }



    public static KafkaConsumer<String, KPIControlMessage> createControlConsumer() {

        KafkaConsumer<String, KPIControlMessage> consumer = new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JacksonDeserializer<>(KPIControlMessage.class));
        List<String> topicNames = Collections.singletonList(CONTROL_TOPIC);
        createTopic(consumerProperties, CONTROL_TOPIC);
        consumer.subscribe(topicNames);
        log.info("Initialized consumer for {}, group name {}, subscribed to {}", KPIModule.class.getCanonicalName(), consumer.groupMetadata().groupId(), topicNames);
        return consumer;
    }

    public static KafkaConsumer<String, KPIData> createDataConsumer() {

        KafkaConsumer<String, KPIData> consumer = new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JacksonDeserializer<>(KPIData.class));
        List<String> topicNames = Collections.singletonList(DATA_TOPIC);
        createTopic(consumerProperties, DATA_TOPIC);
        consumer.subscribe(topicNames);
        log.info("Initialized consumer for {}, group name {}, subscribed to {}", KPIModule.class.getCanonicalName(), consumer.groupMetadata().groupId(), topicNames);
        return consumer;
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
