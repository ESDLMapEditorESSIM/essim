package nl.tno.essim.kpi.data;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class KPIResponseMessage {
    @Data
    @Builder
    public static class SimulationProgress {
        private String simulationID;
        private int messageCount;
    }
    private ModuleInfo moduleInfo;
    private String uuid;
    @Singular
    private List<SimulationProgress> simulationProgresses;
}
