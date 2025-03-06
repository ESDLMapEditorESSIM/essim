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

import esdl.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.Market;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.observation.ObservationField;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.util.ProfileUtils;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ProducerNode extends Node {

    protected Producer producer;
    protected Horizon now;
    protected long timeStep;
    protected double power;
    protected CostInformation costInformation;
    protected RenewableTypeEnum producerType;
    protected ControlStrategy controlStrategy;
    private GenericProfile marginalCostProfile;
    private GenericProfile costProfile;
    private Object producerName;

    protected boolean isRenewable() {
        if (producerType != null) {
            return producerType.equals(RenewableTypeEnum.RENEWABLE);
        }
        return false;
    }

    @Builder(builderMethodName = "producerNodeBuilder")
    public ProducerNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                        int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                        Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        this.producer = (Producer) asset;
        this.producerName = producer.getName() == null ? producer.getId() : producer.getName();
        this.power = producer.getPower();
        this.costInformation = producer.getCostInformation();
        this.producerType = producer.getProdType();
        this.controlStrategy = producer.getControlStrategy();
        if (costInformation != null) {
            marginalCostProfile = costInformation.getMarginalCosts();
        }
        for (Port port : producer.getPort()) {
            if (port instanceof OutPort) {
                costProfile = port.getCarrier().getCost();
                break;
            }
        }
    }

    @Override
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        updateMarketInfo(marketInfo);

        double energyOutput = Double.NaN;

        // Checks if an asset is operational (accounts for Commissioning and
        // Decommissioning date)
        if (!isOperational(now)) {
            makeInflexibleConsumptionFunction(0);
            return;
        }
        for (Port port : producer.getPort()) {
            if (port instanceof OutPort) {
                GenericProfile producerProfile = ProfileUtils.getEnergyProfile(port);
                if (producerProfile != null) {
                    if (ProfileUtils.isPowerProfile(producerProfile)) {
                        energyOutput = timeStep * ProfileUtils.readProfile(producerProfile, now);
                        break;
                    } else if (ProfileUtils.isEnergyProfile(producerProfile)) {
                        energyOutput = ProfileUtils.readProfile(producerProfile, now);
                        break;
                    } else if (ProfileUtils.isPercentageProfile(producerProfile)) {
                        energyOutput = timeStep * ProfileUtils.readProfile(producerProfile, port, now);
                    }
                }
            }
        }
        if (!Double.isNaN(energyOutput)) {
            if (controlStrategy != null && controlStrategy instanceof CurtailmentStrategy) {
                CurtailmentStrategy curtailmentStrategy = (CurtailmentStrategy) controlStrategy;
                energyOutput = Math.min(energyOutput, curtailmentStrategy.getMaxPower() * timeStep);
            }
            makeInflexibleProductionFunction(energyOutput);
        } else {
            energyOutput = timeStep * power;
            if (marginalCostProfile != null) {
                Double aggregateCost = ProfileUtils.readProfile(marginalCostProfile, now);
                if (Double.isNaN(aggregateCost)) {
                    aggregateCost = getDefaultMarginalCost();
                }
                setCost(aggregateCost);
            } else if (costProfile != null) {
                Double aggregateCost = ProfileUtils.readProfile(costProfile, now);
                if (Double.isNaN(aggregateCost)) {
                    aggregateCost = getDefaultMarginalCost();
                }
                setCost(aggregateCost);
            } else {
                log.trace("Producer {} is missing cost information! Defaulting to {}", producerName,
                        getDefaultMarginalCost());
                setCost(getDefaultMarginalCost());
            }

            if (controlStrategy != null && controlStrategy instanceof CurtailmentStrategy) {
                CurtailmentStrategy curtailmentStrategy = (CurtailmentStrategy) controlStrategy;
                energyOutput = Math.min(energyOutput, curtailmentStrategy.getMaxPower() * timeStep);
            }
            makeAdjustableProductionFunction(energyOutput);
        }
    }

    @Override
    public void processAllocation(EssimTime timestamp, ObservationBuilder builder, BigDecimal price) {
        builder.tag("capability", "Producer");
        if (producerType != null) {
            builder.tag("energyType", producerType.toString());
        } else {
            builder.tag("energyType", RenewableTypeEnum.UNDEFINED.toString());
        }

        if (carrier instanceof EnergyCarrier) {
            EnergyCarrier energyCarrier = (EnergyCarrier) carrier;

            if (energyCarrier != null) {
                EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

                double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                        inputEnergyCarrier.getEnergyContentUnit());
                double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                        inputEnergyCarrier.getEmissionUnit());

                if (carrierEnergyContent > Commons.eps) {
                    double inputCarrierQuantity = Math.abs(energy) / carrierEnergyContent;
                    double emission = inputCarrierQuantity * carrierEmission;
                    builder.value(ObservationField.EMISSION, emission);
                    builder.value(ObservationField.FUEL_CONSUMPTION, inputCarrierQuantity);

                    double currentInputCarrierCost = ProfileUtils.readProfile(carrier.getCost(),
                            new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength()));
                    if (!Double.isNaN(currentInputCarrierCost)) {
                        double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                        builder.value(ObservationField.COST, inputCarrierCost);
                    }
                }
            }

        }
        EmissionManager.getInstance(simulationId).addProducer(networkId, producer, Math.abs(energy));
    }

}
