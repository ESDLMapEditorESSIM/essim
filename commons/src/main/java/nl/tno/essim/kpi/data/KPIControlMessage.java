package nl.tno.essim.kpi.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;

@Data
@AllArgsConstructor
public class KPIControlMessage {
    @Data
    @AllArgsConstructor
    public static class Status {
        String uuid;
    }
    @Data
    @AllArgsConstructor
    public static class Start {
        String uuid;
        int messageCount;
        private String simulationId;
        private HashMap<String, String> config;
    }
    @Data
    @AllArgsConstructor
    public static class Stop {
        private String uuid;
        private String simulationId;
    }
    private Status status;
    private Start start;
    private Stop stop;
}
