package nl.tno.essim.kpi;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class SimulationState {
    public enum Status {
        Running,
        Aborted
    }

    private final String simulationID;
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    private final int expectedMessageCount;
    private Status status = Status.Running;
    private KPIProcessor kpiProcessor;

    public SimulationState(String simulationID, int expectedMessageCount, KPIProcessor kpiProcessor) {
        this.simulationID = simulationID;
        this.expectedMessageCount = expectedMessageCount;
        this.kpiProcessor = kpiProcessor;
    }

    public void reset() {
        kpiProcessor.reset();
        messageCounter.set(0);
    }
}
