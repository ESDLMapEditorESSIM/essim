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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import esdl.Carrier;
import esdl.ControlStrategy;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.Storage;
import esdl.StorageStrategy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
public class StorageNode extends Node {

	private Storage storage;
	private EssimDuration timeStepinDT;
	private double maxChargeRate;
	private double maxDischargeRate;
	private double capacity;
	private ControlStrategy controlStrategy;

	@Builder(builderMethodName = "storageNodeBuilder")
	public StorageNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray,
			JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.storage = (Storage) asset;
		this.timeStepinDT = EssimDuration.of(timeStep, ChronoUnit.SECONDS);
		this.maxChargeRate = storage.getMaxChargeRate();
		this.maxDischargeRate = storage.getMaxDischargeRate();
		this.capacity = storage.getCapacity();
		this.controlStrategy = storage.getControlStrategy();
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}
		if (storage.getProfile() != null) {
			double energy = 0.0;
			GenericProfile storageProfile = storage.getProfile();
			if (Commons.isPowerProfile(storageProfile)) {
				energy = timeStep * Commons.aggregatePower(Commons.readProfile(storageProfile, now));
			} else if (Commons.isEnergyProfile(storageProfile)) {
				energy = Commons.aggregateEnergy(Commons.readProfile(storageProfile, now));
			} else if (Commons.isSoCProfile(storageProfile)) {
				LocalDateTime startTime = now.getStartTime().minus(timeStep, ChronoUnit.SECONDS);
				Horizon last = new Horizon(startTime, timeStepinDT);
				double currentSoC = Commons.aggregateSoC(Commons.readProfile(storageProfile, last));
				double newSoC = Commons.aggregateSoC(Commons.readProfile(storageProfile, now));
				energy = ((newSoC - currentSoC) * capacity);
			}

			makeInflexibleConsumptionFunction(energy);

		} else {
			double deltaSoC = energy / capacity;
			double soc = storage.getFillLevel() + deltaSoC;
			storage.setFillLevel(soc);

			double maxChargableEnergy = (1 - soc) * capacity;
			double maxDischargableEnergy = soc * capacity;
			double ecmax = Math.min(maxChargeRate * timeStep, maxChargableEnergy);
			double edmax = Math.min(maxDischargeRate * timeStep, maxDischargableEnergy);
			Double mc1 = null;
			Double mc2 = null;

			if (controlStrategy != null) {
				if (controlStrategy instanceof StorageStrategy) {
					StorageStrategy storageStrategy = (StorageStrategy) controlStrategy;
					GenericProfile marginalChargeCosts = storageStrategy.getMarginalChargeCosts();
					if (marginalChargeCosts != null) {
						mc1 = Commons.aggregateCost(Commons.readProfile(marginalChargeCosts, now));
					}
					GenericProfile marginalDischargeCosts = storageStrategy.getMarginalDischargeCosts();
					if (marginalDischargeCosts != null) {
						mc2 = Commons.aggregateCost(Commons.readProfile(marginalDischargeCosts, now));
					}
				}
			}

			makeStorageFunction(ecmax, edmax, soc, mc1, mc2);
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Storage");
		builder.value("soc", storage.getFillLevel());
		if (energy > 0) {
			EmissionManager.getInstance(simulationId).addConsumer(networkId, storage, Math.abs(energy));
		} else {
			EmissionManager.getInstance(simulationId).addProducer(networkId, storage, Math.abs(energy));
		}
	}
}
