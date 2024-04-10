package nl.tno.essim.kpi;

import nl.tno.essim.kpi.data.KPIData;

import java.util.Map;

public interface KPIProcessor {

    void configure(Map<String, String> configuration, int messageCount);

    void messageReceived(KPIData message);

    String getJSONResult();

    void reset();
}
