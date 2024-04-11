package nl.tno.essim.kpi.data;

import lombok.Data;

import java.util.Map;

@Data
public class KPIData {
    private String measurement;
    private long time;
    private String simulationId;
    private String simulationRun;

    private Map<String, Object> fields;
    private Map<String, String> tags;
}
