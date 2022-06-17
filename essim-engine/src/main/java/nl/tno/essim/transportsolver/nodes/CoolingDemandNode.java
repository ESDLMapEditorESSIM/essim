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
import esdl.CoolingDemand;
import esdl.CostInformation;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.OutPort;
import esdl.Port;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class CoolingDemandNode extends Node {

	private CoolingDemand consumer;
	private Horizon now;
	private long timeStep;
	private double power;
	private OutPort profilePort;
	private CostInformation costInformation;
	private String consumerName;
	private GenericProfile marginalCostProfile;
	private GenericProfile costProfile;

	@Builder(builderMethodName = "consumerNodeBuilder")
	public CoolingDemandNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
			Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
				energy, cost, parent, carrier, children, timeStep, now, connectedPort);
		this.consumer = (CoolingDemand) asset;
		this.consumerName = consumer.getName() == null ? consumer.getId() : consumer.getName();
		this.power = consumer.getPower();
		this.costInformation = consumer.getCostInformation();
		if (costInformation != null) {
			marginalCostProfile = costInformation.getMarginalCosts();
		}
		for (Port port : consumer.getPort()) {
			if (port instanceof OutPort) {
				profilePort = (OutPort) port;
				costProfile = profilePort.getCarrier().getCost();
				break;
			}
		}
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		double energyOutput = Double.NaN;
		GenericProfile convProfile = null;
		if (profilePort != null) {
			convProfile = Commons.getEnergyProfile(profilePort);
		}

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}
		if (convProfile != null) {
			if (Commons.isPowerProfile(convProfile)) {
				energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
			} else if (Commons.isEnergyProfile(convProfile)) {
				energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
			}
		}
		if (!Double.isNaN(energyOutput)) {
			makeInflexibleProductionFunction(energyOutput);
		} else {
			energyOutput = timeStep * power;
			if (marginalCostProfile != null) {
				Double aggregateCost = Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now));
				if(Double.isNaN(aggregateCost)) {
					aggregateCost = DEFAULT_MARGINAL_COST;
				}
			} else if (costProfile != null) {
				Double aggregateCost = Commons.aggregateCost(Commons.readProfile(costProfile, now));
				if(Double.isNaN(aggregateCost)) {
					aggregateCost = DEFAULT_MARGINAL_COST;
				}
				setCost(aggregateCost);
			} else {
				log.warn("CoolingDemand {} is missing cost information! Defaulting to {}", consumerName,
						DEFAULT_MARGINAL_COST);
				setCost(DEFAULT_MARGINAL_COST);
			}
			makeAdjustableProductionFunction(energyOutput);
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Consumer");
		EmissionManager.getInstance(simulationId).addProducer(networkId, consumer, Math.abs(energy));
	}

}
