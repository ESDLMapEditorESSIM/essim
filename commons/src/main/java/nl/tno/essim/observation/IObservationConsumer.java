package nl.tno.essim.observation;

public interface IObservationConsumer {
	void init(String scenarioName);
	void consume(String simulationRunName, IObservationProvider source, IObservation observation);
	void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation);
	void cleanup();
}
