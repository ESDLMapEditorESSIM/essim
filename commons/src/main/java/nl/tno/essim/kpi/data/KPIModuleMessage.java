package nl.tno.essim.kpi.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class KPIModuleMessage {
    private JsonNode result;
    private KPIStatus status;
}
