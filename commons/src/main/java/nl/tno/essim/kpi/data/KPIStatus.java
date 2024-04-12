package nl.tno.essim.kpi.data;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class KPIStatus {
    @Data
    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class SimulationProgress {
        private String simulationID;
        private int messageCount;
        private String statusMessage;
    }
    private ModuleInfo moduleInfo;
    private String uuid;
    @Singular
    private List<SimulationProgress> simulationProgresses;
}
