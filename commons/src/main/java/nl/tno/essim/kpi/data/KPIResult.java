package nl.tno.essim.kpi.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class KPIResult {
    private JsonNode result;
    private String simulationId;
    private String uuid;

}
