package nl.tno.essim.kpi;

import lombok.Getter;
import lombok.Setter;
import nl.tno.essim.kpi.data.KPIStatus;
import nl.tno.essim.kpi.data.ModuleInfo;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

public class KPIModuleState {
    private HashMap<String, KPIStatus.SimulationProgress> simulationProgresses = new HashMap<>();
    @Getter
    private final ModuleInfo moduleInfo;
    @Getter
    @Setter
    private ScheduledFuture future;

    public KPIModuleState(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public void setSimulationProgresses(String simulationId, KPIStatus.SimulationProgress progress) {
        simulationProgresses.put(simulationId, progress);
        // TODO store in mongo?
    }

    public KPIStatus.SimulationProgress getSimulationProgress(String simulationId) {
        return simulationProgresses.get(simulationId);
    }


}
