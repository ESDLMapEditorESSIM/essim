package nl.tno.essim.kpi.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class KPIControlMessage {
    @Data
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Status {
        String uuid;
    }

    @Data
    @Builder
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class Start {
        private String uuid;
        private String kpiId;
        private int messageCount;
        private String simulationId;
        private JsonNode config;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Stop {
        private String uuid;
        private String simulationId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Finished {
        private String simulationId;
    }

    private Status status;
    private Start start;
    private Stop stop;
    private Finished finished;
}
