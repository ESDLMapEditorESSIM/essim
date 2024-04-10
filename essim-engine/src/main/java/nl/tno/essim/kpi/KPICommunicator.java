package nl.tno.essim.kpi;

import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIResponseMessage;
import nl.tno.essim.kpi.data.ModuleInfo;
import nl.tno.essim.model.KPIModuleInfo;

public abstract class KPICommunicator {
    public abstract void sendKPIStart(String simulationID, String... moduleIds);
    public abstract void sendKPIGetStatus(String simulationID);
    public abstract void sendKPIStop(String simulationID);
    public abstract void sendKPIData(KPIData data);

    public void responseReceived(KPIResponseMessage message) {

    }
}
