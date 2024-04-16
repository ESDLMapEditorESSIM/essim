package nl.tno.essim.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.*;

@Slf4j
public abstract class KPICommunicator {
    public enum Type {
        NATS,
        KAFKA
    }
    protected final static String KPI_CONTROL_TOPIC = "KPIControl.request";
    protected final static String KPI_RESPONSE_TOPIC = "KPIControl.response";
    protected final static String OBSERVATION_PREFIX = "observation.";
    protected final ObjectMapper objectMapper;
    private final KPIManager kpiManager;

    public abstract void sendData(KPIData data);
    protected abstract void sendControlMessage(KPIControlMessage message);

    public abstract Type getType();

    public KPICommunicator(KPIManager kpiManager) {
        this.kpiManager = kpiManager;
        objectMapper = new ObjectMapper();
    }

    protected void responseReceived(KPIModuleMessage message) {
        log.trace("Message received: {}", message);
        if (message.getStatus() != null) {
            kpiManager.processKPIMessage(message.getStatus());
        }
        if (message.getResult() != null) {
            kpiManager.resultReceived(message.getResult());
        }
    }

    public void sendKPIStart(KPIInfo info) {
        info.getKpiCalculators().forEach(kc -> {
            KPIControlMessage start = KPIControlMessage.builder().start(KPIControlMessage.Start.builder().simulationId(info.getSimulationId()).messageCount((int) info.getNoOfMessages()).config(kc.getConfig()).build()).build();
            sendControlMessage(start);
        });
    }

    public void sendKPIGetStatus() {
        KPIControlMessage message = KPIControlMessage.builder().status(KPIControlMessage.Status.builder().build()).build();
        log.trace("sendKPIGetStatus: {}", message);
        sendControlMessage(message);
    }

    public void sendKPIStop(String simulationID) {
        KPIControlMessage message = KPIControlMessage.builder().stop(KPIControlMessage.Stop.builder().simulationId(simulationID).build()).build();
        sendControlMessage(message);
    }
}
