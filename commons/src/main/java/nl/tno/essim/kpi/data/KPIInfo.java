package nl.tno.essim.kpi.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class KPIInfo {
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class KpiCalculator {
        private String id;
        private HashMap<String, String> config;
    }

    private String simulationId;
    private long noOfMessages;
    private ArrayList<KpiCalculator> kpiCalculators;
}

