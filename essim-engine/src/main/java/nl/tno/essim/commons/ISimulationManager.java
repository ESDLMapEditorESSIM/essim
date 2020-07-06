package nl.tno.essim.commons;

import java.time.LocalDateTime;
import java.util.List;

import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.transportsolver.TransportSolver;

public interface ISimulationManager {

	String getName();

	public void addSolvers(List<TransportSolver> solvers);

	void setObservationConsumers(List<IObservationConsumer> consumers);

	LocalDateTime getStartDateTime();

	LocalDateTime getEndDateTime();

}