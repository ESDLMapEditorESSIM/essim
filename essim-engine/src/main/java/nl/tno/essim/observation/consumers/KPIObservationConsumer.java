package nl.tno.essim.observation.consumers;

import nl.tno.essim.kpi.KPICommunicator;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

public class KPIObservationConsumer implements IObservationConsumer {
    private final KPICommunicator kpiCommunicator;
    private long timestamp;
    private String simulationId;

    public KPIObservationConsumer(KPICommunicator kpiCommunicator) {
        this.kpiCommunicator = kpiCommunicator;
    }

    @Override
    public void init(String simulationID) {
        this.simulationId = simulationID;
    }

    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
        if (source.getProviderName()
                .equals("ESSIM")) {
            return;
        }
        KPIData kpiData = new KPIData();


        kpiData.setMeasurement(source.getProviderName());
        timestamp = observation.getObservedAt()
                .atZone(EssimTime.defaultTimeZone)
                .toEpochSecond();
        kpiData.setTime(timestamp);
        kpiData.setFields(observation.getValues());
        kpiData.setTags(observation.getTags());
        kpiData.setSimulationRun(simulationRunName);
        kpiCommunicator.sendData(kpiData);
    }

    @Override
    public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {

    }

    @Override
    public void cleanup() {
        kpiCommunicator.sendKPIStop(simulationId);
    }
}
