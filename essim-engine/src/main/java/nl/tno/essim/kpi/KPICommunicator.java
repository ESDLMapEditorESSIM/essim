package nl.tno.essim.kpi;

import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIInfo;
import nl.tno.essim.kpi.data.KPIResponseMessage;
import nl.tno.essim.kpi.data.KPIResultMessage;

public abstract class KPICommunicator {
    private final KPIManager kpiManager;

    public abstract void sendKPIStart(KPIInfo info);

    public abstract void sendKPIGetStatus();

    public abstract void sendKPIStop(String simulationID);

    public abstract void sendKPIData(KPIData data);

    public KPICommunicator(KPIManager kpiManager) {
        this.kpiManager = kpiManager;
    }

    protected void responseReceived(KPIResponseMessage message) {
        kpiManager.processKPIMessage(message);
    }

    protected void resultReceived(KPIResultMessage result) {
        kpiManager.resultReceived(result);
    }
}
