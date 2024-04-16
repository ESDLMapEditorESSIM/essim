package nl.tno.essim.kpi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonDeserializer;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.*;
import nl.tno.essim.kpi.kpi.KPIProcessorFactory;
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
public class KPIModule {
    private final static Duration POLL_DURATION = Duration.ofSeconds(1);
    private final static Properties consumerProperties;
    private final static int PARTITIONS = 1;
    private KafkaProducer<String, KPIModuleMessage> producer;
    private static final String DATA_TOPIC = "observation.all";
    public final static String CONTROL_TOPIC = "KPIControl.request";
    public final static String KPI_RESPONSE_TOPIC = "KPIControl.response";
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

    private final KPIProcessorFactory kpiProcessorFactory;
    ObjectMapper objectMapper;
    private final String uuid;
    //    private KafkaConsumer<String, KPIData> dataConsumer;
    private final HashMap<String, SimulationState> simulationStateMap = new HashMap<>();

    public KPIModule(KPIProcessorFactory kpiProcessorFactory) {
        this.kpiProcessorFactory = kpiProcessorFactory;
        uuid = UUID.randomUUID().toString();
        objectMapper = new ObjectMapper();
    }

    public void start() {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "0");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-" + kpiProcessorFactory.getModuleInfo().getTypeId());

        producer = new KafkaProducer<>(producerProperties, new StringSerializer(), new JacksonSerializer<>());

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

    private void handleKPIControlMessage(KPIControlMessage controlMessage) {
        if (controlMessage.getStatus() != null) {
            handleStatus(controlMessage.getStatus());
        }
        if (controlMessage.getStop() != null) {
            handleStop(controlMessage.getStop());
        }
        if (controlMessage.getStart() != null) {
            handleStart(controlMessage.getStart());
        }
    }

    private void handleStart(KPIControlMessage.Start start) {
        SimulationState simulationState = simulationStateMap.get(start.getSimulationId());
        if (simulationState != null) {
            log.info("Restarting KPI calculations for simulation id {}", start.getSimulationId());
        } else {
            log.info("Starting KPI calculations for simulation id {}, expecting {} messages", start.getSimulationId(), start.getMessageCount());
        }
        simulationState = new SimulationState(start.getSimulationId(), start.getMessageCount(), kpiProcessorFactory.createKPIProcessor(start.getSimulationId()));
        simulationStateMap.put(start.getSimulationId(), simulationState);
        simulationState.getKpiProcessor().configure(start.getConfig(), start.getMessageCount());
    }

    private void handleStop(KPIControlMessage.Stop stop) {
        if (stop.getUuid() == null || uuid.equals(stop.getUuid())) {
            reset(stop.getSimulationId());
        }
    }

    private void reset(String simulationId) {
        SimulationState simulationState = simulationStateMap.get(simulationId);
        if (simulationState != null) {
            simulationState.reset();
        }
    }

    private void handleStatus(KPIControlMessage.Status status) {
        if (status.getUuid() == null || uuid.equals(status.getUuid())) {
            sendStatus();
        }
    }

    private void sendStatus() {
        KPIStatus.KPIStatusBuilder builder = KPIStatus.builder().uuid(uuid).moduleInfo(kpiProcessorFactory.getModuleInfo());
        for (var e : simulationStateMap.entrySet()) {
            int messageCount = e.getValue().getMessageCounter().get();
            double progress = e.getValue().getExpectedMessageCount() == 0 ? 0 : (double) messageCount / e.getValue().getExpectedMessageCount();
            String statusMessage = "Not yet started";
            if (messageCount >= e.getValue().getExpectedMessageCount()) {
                statusMessage = "Finished";
            } else if (messageCount > 0) {
                statusMessage = "Running";
            }
            builder.simulationProgress(KPIStatus.SimulationProgress.builder().simulationID(e.getKey()).messageCount(messageCount).progress(progress).statusMessage(statusMessage).build());
        }
        KPIStatus status = builder.build();
        log.trace("Sending message {}", status);
        KPIModuleMessage m = new KPIModuleMessage();
        m.setStatus(status);
        sendMessage(m);
    }


    private void sendMessage(KPIModuleMessage m) {
        producer.send(new ProducerRecord<>(KPI_RESPONSE_TOPIC, uuid, m));
    }

    private void handleDataMessage(String key, KPIData data) {
        String simulationId = data.getSimulationId();
        SimulationState simulationState = simulationStateMap.get(simulationId);
        if (simulationState != null) {
            int messageCount = simulationState.getMessageCounter().incrementAndGet();
            if (messageCount % 1000 == 0) {
                log.trace("{} messages for simulation {}", messageCount, simulationId);
            }
            simulationState.getKpiProcessor().messageReceived(data);
            if (messageCount == simulationState.getExpectedMessageCount()) {
                log.debug("{} messages received for simulation {}", messageCount, simulationId);
                log.info("Data for simulation run {} complete, sending results", simulationId);
                String result = simulationState.getKpiProcessor().getJSONResult();
                try {
                    KPIModuleMessage message = new KPIModuleMessage();
                    message.setResult(objectMapper.readTree(result));
                    sendMessage(message);
                } catch (JsonProcessingException e) {
                    log.warn("Could not parse result json {}", result, e);
                }
            }
        } else {
            log.warn("No simulation found for id {}", simulationId);
        }

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
