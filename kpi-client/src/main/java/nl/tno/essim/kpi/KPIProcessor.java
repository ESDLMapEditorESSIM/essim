package kpimodule;

import nl.tno.kpimodule.data.KPIData;

import java.util.Map;

public interface KPIProcessor {

    void configure(Map<String, String> configuration, int messageCount);

    void messageReceived(KPIData message);

    String getJSONResult();

    void reset();
}
