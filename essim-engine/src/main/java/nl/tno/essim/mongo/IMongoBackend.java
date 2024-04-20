package nl.tno.essim.mongo;

import java.util.HashMap;
import java.util.List;

import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.*;

public interface IMongoBackend {

    HashMap<String, IStatusProvider> getStatusMap();

    void updateStatus(String status);

    String getStatus();

    String addSimulation(EssimSimulation simulation);

    void updateSimulationData(String simulationId, EssimSimulation simulationData);

    SimulationStatus getSimulationStatus(String simulationId);

    void updateSimulationStatus(String simulationId, Status type, String status);

    List<EssimSimulation> getSimulations();

    EssimSimulation getSimulation(String simulationId);

    void stop();

    String getMetaInfo(String key);

    void updateMetaInfo(String key, String value);

    List<KPIModuleInfo> getAllKpiModules();

    KPIModule getKPIForSimulation(String simulationId, String kpiId);

}