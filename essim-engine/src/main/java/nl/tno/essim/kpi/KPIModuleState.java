package nl.tno.essim.kpi;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nl.tno.essim.kpi.data.KPIResponseMessage;
import nl.tno.essim.kpi.data.ModuleInfo;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

public class KPIModuleState {
    private HashMap<String, KPIResponseMessage.SimulationProgress> simulationProgresses = new HashMap<>();
    @Getter
    private final ModuleInfo moduleInfo;
    @Getter
    @Setter
    private ScheduledFuture future;

    public KPIModuleState(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public void setSimulationProgresses(String simulationId, KPIResponseMessage.SimulationProgress progress) {
        simulationProgresses.put(simulationId, progress);
        // TODO store in mongo?
    }


}
