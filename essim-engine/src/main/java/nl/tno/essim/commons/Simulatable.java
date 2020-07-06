/**
 */
package nl.tno.essim.commons;

import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.time.EssimTime;

public interface Simulatable {
	void init(EssimTime timestamp);
	void step(EssimTime timestamp);
	void stop();
	void reset();
	SimulationStatus getState();
	void pause();
	void setSimulationManager(ISimulationManager manager);
	void setObservationManager(IObservationManager manager);

} // Simulatable
