package nl.tno.essim.kpi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIResult;
import nl.tno.essim.kpi.data.KPIStatus;
import nl.tno.essim.kpi.data.ModuleInfo;
import nl.tno.essim.model.KPIModuleInfo;
import nl.tno.essim.model.KPIModuleInfoImpl;
import nl.tno.essim.mongo.MongoBackend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KPIManager {
    private final static int STATUS_INTERVAL = 1; // seconds
    private final static int MAX_OFFLINE = 20; // seconds
    public static String KAFKA_URL;
    public static String NATS_URL;
    public static String RABBITMQ_URL;

    static {
        KAFKA_URL = System.getenv("KAFKA_URL");
        NATS_URL = System.getenv("NATS_URL");
        RABBITMQ_URL = System.getenv("RABBITMQ_URL");
    }

    @Getter
    private final static KPIManager instance = new KPIManager();
    private final ScheduledExecutorService executor;
    private final HashMap<String, KPICommunicator> communicatorMap = new HashMap<>();
    private final HashMap<String, KPIModuleState> moduleStates = new HashMap<>();
    private final static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public void resultReceived(KPIResult result) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("KPI Result received : {}", objectMapper.writeValueAsString(result));
            }
            JsonNode jsonNode = objectMapper.readTree(result.getResult().asText());
            KPIModuleState kpiModuleState = moduleStates.get(result.getUuid());
            ((ObjectNode) jsonNode).put("status", "Success");
            MongoBackend.getInstance().updateKPIForSimulation(result.getSimulationId(), kpiModuleState.getModuleInfo().getTypeId(), jsonNode.toString(), "success");
        } catch (JsonProcessingException e) {
            log.warn("Invalid json", e);
        }
    }

    private class StateRemover implements Runnable {
        private final String uuid;


        public StateRemover(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void run() {
            moduleStates.remove(uuid);
        }

    }

    public KPIManager() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::queryStatus, STATUS_INTERVAL, STATUS_INTERVAL, TimeUnit.SECONDS);
        if (KAFKA_URL != null) {
            KafkaKPICommunicator communicator = new KafkaKPICommunicator(this, KAFKA_URL);
            communicatorMap.put(KAFKA_URL, communicator);
            communicator.sendKPIGetStatus();
        }
        if (NATS_URL != null) {
            NATSKPICommunicator communicator = new NATSKPICommunicator(this, NATS_URL);
            communicatorMap.put(NATS_URL, communicator);
            communicator.sendKPIGetStatus();
        }
        if (RABBITMQ_URL != null) {
            RabbitMQKPICommunicator communicator = new RabbitMQKPICommunicator(this, RABBITMQ_URL);
            communicatorMap.put(RABBITMQ_URL, communicator);
            communicator.sendKPIGetStatus();
        }
    }

    public void queryStatus() {
        for (KPICommunicator c : communicatorMap.values()) {
            c.sendKPIGetStatus();
        }
    }

    public void processKPIStatus(KPIStatus message) {
        log.trace("KPI status received: {}", message);
        KPIModuleState moduleState = moduleStates.get(message.getUuid());
        if (moduleState == null) {
            log.info("Registering KPI module '{}' ({}): {}", message.getModuleInfo().getTitle(), message.getModuleInfo().getTypeId(), message.getModuleInfo().getDescription());
            moduleState = new KPIModuleState(message.getModuleInfo());
            moduleStates.put(message.getUuid(), moduleState);
        }
        ScheduledFuture<?> future = moduleState.getFuture();
        if (future != null) {
            future.cancel(false);
        }
        future = executor.schedule(new StateRemover(message.getUuid()), MAX_OFFLINE, TimeUnit.SECONDS);
        moduleState.setFuture(future);
        if (message.getSimulationProgresses() != null) {
            for (KPIStatus.SimulationProgress p : message.getSimulationProgresses()) {
                moduleState.setSimulationProgresses(p.getSimulationID(), p);
            }
        }
    }

    public List<KPICommunicator> getKPICommunicators() {
        return new ArrayList<>(communicatorMap.values());
    }

    public KPIStatus.SimulationProgress getKPIModuleInfo(String typeId, String simulationId) {
        for (var state : moduleStates.values()) {
            if (state.getModuleInfo().getTypeId().equals(typeId)) {
                return state.getSimulationProgress(simulationId);
            }
        }
        return null;
    }


    public List<KPIModuleInfo> getAllKPIModules() {
        ArrayList<KPIModuleInfo> infos = new ArrayList<>();
        for (var entry : moduleStates.entrySet()) {
            KPIModuleState moduleState = entry.getValue();
            KPIModuleInfoImpl info = createKpiModuleInfo(moduleState);
            infos.add(info);
        }
        return infos;
    }

    private static KPIModuleInfoImpl createKpiModuleInfo(KPIModuleState moduleState) {
        KPIModuleInfoImpl info = new KPIModuleInfoImpl();
        ModuleInfo moduleInfo = moduleState.getModuleInfo();
        info.setStatus("registered");
        info.setDescription(moduleInfo.getDescription());
        info.setSampleOutput(moduleInfo.getSampleOutput());
        info.setCalculatorId(moduleInfo.getTypeId());
        info.setConfigConfig(moduleInfo.getConfig());
        info.setTitle(moduleInfo.getTitle());
        info.setUpSince(moduleInfo.getUpSince());
        return info;
    }

}
