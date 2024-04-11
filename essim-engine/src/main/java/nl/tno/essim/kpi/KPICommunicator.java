package nl.tno.essim.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.tno.essim.kpi.data.*;

public abstract class KPICommunicator {
    protected final static String KPI_CONTROL_TOPIC = "KPIControl.request";
    protected final static String KPI_RESPONSE_TOPIC = "KPIControl.response";
    protected final static String OBSERVATION_PREFIX = "observation.";
    protected final ObjectMapper objectMapper;
    private final KPIManager kpiManager;

    public abstract void sendKPIData(KPIData data);
    protected abstract void sendControlMessage(KPIControlMessage message);

    public KPICommunicator(KPIManager kpiManager) {
        this.kpiManager = kpiManager;
        objectMapper = new ObjectMapper();
    }

    protected void responseReceived(KPIStatus message) {
        kpiManager.processKPIMessage(message);
    }

    protected void resultReceived(KPIResult result) {
        kpiManager.resultReceived(result);
    }

    public void sendKPIStart(KPIInfo info) {
        info.getKpiCalculators().stream().forEach(kc -> {
            KPIControlMessage start = KPIControlMessage.builder().start(KPIControlMessage.Start.builder().simulationId(info.getSimulationId()).messageCount((int) info.getNoOfMessages()).config(kc.getConfig()).build()).build();
            sendControlMessage(start);
        });
    }

    public void sendKPIGetStatus() {
        KPIControlMessage message = KPIControlMessage.builder().status(KPIControlMessage.Status.builder().build()).build();
        sendControlMessage(message);
    }

    public void sendKPIStop(String simulationID) {
        KPIControlMessage message = KPIControlMessage.builder().stop(KPIControlMessage.Stop.builder().simulationId(simulationID).build()).build();
        sendControlMessage(message);
    }
}
