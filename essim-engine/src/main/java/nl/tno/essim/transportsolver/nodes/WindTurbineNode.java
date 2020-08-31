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
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.CurtailmentStrategy;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.OutPort;
import esdl.Port;
import esdl.WindTurbine;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
public class WindTurbineNode extends ProducerNode {

	private WindTurbine windTurbine;

	public WindTurbineNode(String simulationId, String nodeId, String address, String networkId,
			JSONArray animationArray, JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.windTurbine = (WindTurbine) asset;
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		double energyOutput = Double.NaN;
		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}
		for (Port port : windTurbine.getPort()) {
			if (port instanceof OutPort) {
				GenericProfile profile = Commons.getEnergyProfile(port);
				if (profile != null) {
					if (Commons.isPowerProfile(profile)) {
						energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
						break;
					} else if (Commons.isEnergyProfile(profile)) {
						energyOutput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
						break;
					}
				}
			}
		}
		if (Double.isNaN(energyOutput)) {
			energyOutput = timeStep * power;
		}
		// Curtailment strategy implementation
		// The producer does not produce more than its threshold output power
		if (controlStrategy != null && controlStrategy instanceof CurtailmentStrategy) {		
			CurtailmentStrategy curtailmentStrategy = (CurtailmentStrategy) controlStrategy;
			energyOutput = Math.min(energyOutput, curtailmentStrategy.getMaxPower() * timeStep);
		}

		makeInflexibleProductionFunction(energyOutput);
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		super.processAllocation(timestamp, builder, price);
	}
}
