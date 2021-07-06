/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.transportsolver.nodes;

import java.util.List;

import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.Port;
import esdl.Transport;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransportNode extends Node {

	private Transport transport;
	private double capacity;

	@Builder(builderMethodName = "transportNodeBuilder")
	public TransportNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			int directionFactor, Role role, BidFunction demandFunction, double energy, double cost, Node parent,
			Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction, energy, cost,
				parent, carrier, children, timeStep, now, connectedPort);
		this.transport = (Transport) asset;
		this.capacity = transport.getCapacity();
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now) {
	};

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		long timeStepInSeconds = timestamp.getSimulationStepLength().getSeconds();
		builder.tag("capability", "Transport");
		builder.value("capacity", capacity).value("allocationEnergy", directionFactor * energy).value("allocationPower",
				directionFactor * energy / timeStepInSeconds);
		if (capacity != 0) {
			double load = directionFactor * energy / (timeStepInSeconds * capacity);
			builder.value("load", load);
		}
	}

}
