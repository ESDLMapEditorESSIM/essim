package nl.tno.essim.kpi.kpi;

import nl.tno.essim.kpi.KPIProcessor;

public interface KPIProcessorFactory {
    KPIProcessor createKPIProcessor(String simulationID);
    nl.tno.essim.kpi.data.ModuleInfo getModuleInfo();
}
