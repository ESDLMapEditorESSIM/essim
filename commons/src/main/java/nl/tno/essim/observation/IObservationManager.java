package nl.tno.essim.observation;

import java.util.List;

public interface IObservationManager {

	public void registerConsumer(IObservationConsumer consumer);

	public void registerConsumers(List<IObservationConsumer> consumerList);

	public void publish(IObservationProvider source, IObservation observation);

	public void publishBatch(IObservationProvider source, IObservation observation);

}
