package nl.tno.essim.kpi.data;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("status")
        private String statusMessage;
        private double progress;
    }
    private ModuleInfo moduleInfo;
    private String uuid;
    @Singular
    private List<SimulationProgress> simulationProgresses;
}
