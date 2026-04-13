package nl.tno.essim.kpi.data;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class KPIInfo {
    @Data
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class KpiCalculator {
        private String id;
        private JsonNode config;
    }

    private String simulationId;
    private long noOfMessages;
    @Singular
    private List<KpiCalculator> kpiCalculators;
}

