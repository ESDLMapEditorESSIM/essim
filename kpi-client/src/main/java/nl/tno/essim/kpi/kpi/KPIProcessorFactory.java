package kpimodule.kpi;

import nl.tno.kpimodule.KPIProcessor;
import nl.tno.kpimodule.data.ModuleInfo;

public interface KPIProcessorFactory {
    KPIProcessor createKPIProcessor(String simulationID);
    ModuleInfo getModuleInfo();
}
