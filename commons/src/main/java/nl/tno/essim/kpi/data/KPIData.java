package nl.tno.essim.kpi.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class KPIData {
    private String measurement;
    private long time;
    private String simulationId;
    private String simulationRun;

    private Map<String, Object> fields;
    private Map<String, String> tags;
}