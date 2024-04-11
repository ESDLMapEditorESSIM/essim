package nl.tno.essim.kpi.data;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class KPIInfo {
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class KpiCalculator {
        private String id;
        private Map<String, String> config;
    }

    private String simulationId;
    private long noOfMessages;
    @Singular
    private List<KpiCalculator> kpiCalculators;
}

