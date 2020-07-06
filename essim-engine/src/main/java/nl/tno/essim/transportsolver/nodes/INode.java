package nl.tno.essim.transportsolver.nodes;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

public interface INode {

	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice);
	
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price);
}
