package nl.tno.essim.kpi;

public interface KPIProcessorFactory {
    KPIProcessor createKPIProcessor(String simulationID);
    nl.tno.essim.kpi.data.ModuleInfo getModuleInfo();
}
