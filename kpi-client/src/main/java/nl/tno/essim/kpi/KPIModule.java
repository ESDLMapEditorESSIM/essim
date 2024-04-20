package nl.tno.essim.kpi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;
import nl.tno.essim.kpi.data.KPIStatus;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
public abstract class KPIModule {
    public final static String CONTROL_TOPIC = "KPIControl.request";
    public final static String KPI_RESPONSE_TOPIC = "KPIControl.response";

    protected final KPIProcessorFactory kpiProcessorFactory;
    protected ObjectMapper objectMapper;
    protected final String uuid;
    private final HashMap<String, SimulationState> simulationStateMap = new HashMap<>();

    public KPIModule(KPIProcessorFactory kpiProcessorFactory) {
        this.kpiProcessorFactory = kpiProcessorFactory;
        uuid = UUID.randomUUID().toString();
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }


    protected abstract void sendMessage(KPIModuleMessage m);

    public abstract void start();

    protected void handleKPIControlMessage(KPIControlMessage controlMessage) {
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

    protected void sendStatus() {
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

    protected void handleDataMessage(String key, KPIData data) {
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

}
