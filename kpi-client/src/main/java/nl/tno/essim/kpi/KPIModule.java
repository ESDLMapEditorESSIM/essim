package nl.tno.essim.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonDeserializer;
import com.github.jcustenborder.kafka.serialization.jackson.JacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;
import nl.tno.essim.kpi.data.KPIStatus;
import nl.tno.essim.kpi.kpi.KPIProcessorFactory;
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

@Slf4j
public class KPIModule {
    private final static Duration POLL_DURATION = Duration.ofSeconds(1);
    private final static Properties consumerProperties;
    //    private final static int BATCH_SIZE = 2000; // whichever is first, before publishing
    private final static int PARTITIONS = 3;
    private KafkaProducer<String, KPIModuleMessage> producer;
    private static final String OBSERVATION_TOPIC = "observation";
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
    private String uuid;
    private KafkaConsumer<String, KPIControlMessage> controlConsumer;
    private KafkaConsumer<String, KPIData> dataConsumer;
    private HashMap<String, SimulationState> simulationStateMap = new HashMap<>();

    public KPIModule(KPIProcessorFactory kpiProcessorFactory) {
        this.kpiProcessorFactory = kpiProcessorFactory;
        uuid = UUID.randomUUID().toString();
        objectMapper = new ObjectMapper();
    }

    public void start() {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
//        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "0");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "ESSIM-" + kpiProcessorFactory.getModuleInfo().getTypeId());

        producer = new KafkaProducer<>(producerProperties, new StringSerializer(), new JacksonSerializer<>());

        controlConsumer = createControlConsumer();
        dataConsumer = createDataConsumer();
        new Thread(() -> {
            while (true) {
                ConsumerRecords<String, KPIData> records = dataConsumer.poll(POLL_DURATION);
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
            log.info("Starting KPI calculations for simulation id {}", start.getSimulationId());
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
        simulationStateMap.get(simulationId).reset();
    }

    private void handleStatus(KPIControlMessage.Status status) {
        if (status.getUuid() == null || uuid.equals(status.getUuid())) {
            KPIStatus.KPIStatusBuilder builder = KPIStatus.builder().uuid(uuid).moduleInfo(kpiProcessorFactory.getModuleInfo());
            for (var e : simulationStateMap.entrySet()) {
                builder.simulationProgress(KPIStatus.SimulationProgress.builder().simulationID(e.getKey()).messageCount(e.getValue().getMessageCounter().get()).build());
            }
            sendMessage(builder.build());
        }
    }


    private void sendMessage(KPIStatus status) {
        KPIModuleMessage m = new KPIModuleMessage();
        m.setStatus(status);
        producer.send(new ProducerRecord<>(uuid, m));
    }

    private void handleDataMessage(String simulationId, KPIData message) {
        SimulationState simulationState = simulationStateMap.get(simulationId);
        if (simulationState != null) {
            int messageCount = simulationState.getMessageCounter().incrementAndGet();
            simulationState.getKpiProcessor().messageReceived(message);
            if (messageCount == simulationState.getExpectedMessageCount()) {
                log.info("Data for simulation run {} complete, sending results", simulationId);
                String result = simulationState.getKpiProcessor().getJSONResult();
                // TODO send result
            }
        }

    }

    public static KafkaConsumer<String, KPIControlMessage> createControlConsumer() {

        KafkaConsumer<String, KPIControlMessage> consumer = new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JacksonDeserializer<>(KPIControlMessage.class));
        List<String> topicNames = Collections.singletonList("KPIControl.request");
        consumer.subscribe(topicNames);
        log.info("Initialized consumer for " + KPIModule.class.getCanonicalName() + ", group name " + consumer.groupMetadata().groupId() + ", subscribed to " + topicNames);
        return consumer;
    }

    public static KafkaConsumer<String, KPIData> createDataConsumer() {

        KafkaConsumer<String, KPIData> consumer = new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JacksonDeserializer<>(KPIData.class));
        List<String> topicNames = Collections.singletonList(".*observation");
        consumer.subscribe(topicNames);
        log.info("Initialized consumer for " + KPIModule.class.getCanonicalName() + ", group name " + consumer.groupMetadata().groupId() + ", subscribed to " + topicNames);
        return consumer;
    }


}
