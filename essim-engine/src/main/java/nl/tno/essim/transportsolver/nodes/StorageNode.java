/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */

package nl.tno.essim.transportsolver.nodes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import esdl.Carrier;
import esdl.ControlStrategy;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.Port;
import esdl.Storage;
import esdl.StorageStrategy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.Market;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.observation.ObservationField;
import nl.tno.essim.profiles.ProfileException;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.profiles.ProfileUtils;
import org.slf4j.MarkerFactory;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class StorageNode extends Node {

    private Storage storage;
    private EssimDuration timeStepinDT;
    private double maxChargeRate;
    private double maxDischargeRate;
    private double capacity;
    private ControlStrategy controlStrategy;

    @Builder(builderMethodName = "storageNodeBuilder")
    public StorageNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                       int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                       Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);

        this.storage = (Storage) asset;
        this.timeStepinDT = EssimDuration.of(timeStep, ChronoUnit.SECONDS);
        this.maxChargeRate = storage.getMaxChargeRate();
        this.maxDischargeRate = storage.getMaxDischargeRate();
        this.capacity = storage.getCapacity();
        this.controlStrategy = storage.getControlStrategy();
    }

    @Override
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        updateMarketInfo(marketInfo);
        // Checks if an asset is operational (accounts for Commissioning and
        // Decommissioning date)
        if (!isOperational(now)) {
            makeInflexibleConsumptionFunction(0);
            return;
        }
        if (storage.getProfile() != null) {
            double energy = 0.0;
            GenericProfile storageProfile = storage.getProfile();
            try {
                if (ProfileUtils.isPowerProfile(storageProfile)) {
                    energy = timeStep * profileDataManager.readProfile(storageProfile, now);
                } else if (ProfileUtils.isEnergyProfile(storageProfile)) {
                    energy = profileDataManager.readProfile(storageProfile, now);
                } else if (ProfileUtils.isSoCProfile(storageProfile)) {
                    LocalDateTime startTime = now.getStartTime().minus(timeStep, ChronoUnit.SECONDS);
                    Horizon last = new Horizon(startTime, timeStepinDT);
                    double currentSoC = profileDataManager.readProfile(storageProfile, last);
                    double newSoC = profileDataManager.readProfile(storageProfile, now);
                    energy = ((newSoC - currentSoC) * capacity);
                }
            } catch (Exception e) {
                log.error(MarkerFactory.getMarker(simulationId), "Error while reading profile for storage {}: {}", storage.getName(), e.getMessage());
                throw new RuntimeException(e);
            }

            makeInflexibleConsumptionFunction(energy);

        } else {
            double deltaSoC = energy / capacity;
            double soc = storage.getFillLevel() + deltaSoC;
            if (soc > 1) {
                log.warn(MarkerFactory.getMarker(simulationId), "SOC > 1 : {}", soc);
                soc = 1;
            }
            storage.setFillLevel(soc);

            double maxChargableEnergy = (1 - soc) * capacity;
            double maxDischargableEnergy = soc * capacity;
            double ecmax = Math.min(maxChargeRate * timeStep, maxChargableEnergy);
            double edmax = Math.min(maxDischargeRate * timeStep, maxDischargableEnergy);
            Double mcCharge = null;
            Double mcDischarge = null;

            if (controlStrategy != null) {
                if (controlStrategy instanceof StorageStrategy) {
                    try {
                        StorageStrategy storageStrategy = (StorageStrategy) controlStrategy;
                        GenericProfile marginalChargeCosts = storageStrategy.getMarginalChargeCosts();
                        if (marginalChargeCosts != null) {
                            mcCharge = profileDataManager.readProfile(marginalChargeCosts, now);
                        }
                        GenericProfile marginalDischargeCosts = storageStrategy.getMarginalDischargeCosts();
                        if (marginalDischargeCosts != null) {
                            mcDischarge = profileDataManager.readProfile(marginalDischargeCosts, now);
                        }
                    } catch (ProfileException e) {
                        log.error(MarkerFactory.getMarker(simulationId), "Error while reading marginal cost profile for storage {}: {}", storage.getName(), e.getMessage());
                    }
                }
            }

            makeStorageFunction(ecmax, edmax, soc, mcCharge, mcDischarge);
        }
    }

    @Override
    public void processAllocation(EssimTime timestamp, ObservationBuilder builder) {
        builder.tag("capability", "Storage");
        builder.value(ObservationField.SOC, storage.getFillLevel());
        if (energy > 0) {
            EmissionManager.getInstance(simulationId).addConsumer(networkId, storage, Math.abs(energy));
        } else {
            EmissionManager.getInstance(simulationId).addProducer(networkId, storage, Math.abs(energy));
        }
    }
}
