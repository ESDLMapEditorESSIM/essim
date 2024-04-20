package nl.tno.essim.kpi;

import com.fasterxml.jackson.databind.JsonNode;
import nl.tno.essim.kpi.data.KPIData;

import java.util.Map;

public interface KPIProcessor {

    void configure(JsonNode configuration, int messageCount);

    void messageReceived(KPIData message);

    String getJSONResult();

    void reset();
}
