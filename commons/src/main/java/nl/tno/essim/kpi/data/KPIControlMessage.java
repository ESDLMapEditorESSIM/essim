package nl.tno.essim.kpi.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class KPIControlMessage {
    @Data
    @AllArgsConstructor
    @Builder
    public static class Status {
        String uuid;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class Start {
        String uuid;
        int messageCount;
        private String simulationId;
        private Map<String, String> config;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class Stop {
        private String uuid;
        private String simulationId;
    }

    private Status status;
    private Start start;
    private Stop stop;
}
