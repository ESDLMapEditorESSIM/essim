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

import java.util.List;

import esdl.AbstractBasicConversion;
import esdl.Carrier;
import esdl.ControlStrategy;
import esdl.CostInformation;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.DrivenBySupply;
import esdl.EnergyAsset;
import esdl.EnergyCarrier;
import esdl.GenericProfile;
import esdl.InPort;
import esdl.OutPort;
import esdl.Port;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.Market;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class AbstractBasicConversionNode extends Node {

    @Value
    public static class PowerPricePair {
        private GenericProfile priceProfile;
        private double maxPower;
    }

    protected static final double DEFAULT_CONVERSION_EFFICIENCY = 0.6;
    protected AbstractBasicConversion conversion;
    protected double efficiency;
    protected double power;
    protected CostInformation costInformation;
    protected ControlStrategy controlStrategy;
    private InPort inputPort;
    private Carrier inputCarrier;
    private List<PowerPricePair> inputPrices;
    private OutPort outputPort;
    private Carrier outputCarrier;
    private List<PowerPricePair> outputPrices;
    private Float outputPrice;
    private boolean specialConversion;
    private String conversionName;
    private GenericProfile marginalCostProfile;
    private GenericProfile costProfile;

    @Builder(builderMethodName = "abstractBasicConversionNodeBuilder")
    public AbstractBasicConversionNode(String simulationId, String nodeId, String address, String networkId,
                                       EnergyAsset asset, String esdlString, int directionFactor, Role role, BidFunction demandFunction,
                                       double energy, double cost, Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now,
                                       Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        this.conversion = (AbstractBasicConversion) asset;
        this.conversionName = asset.getName() == null ? asset.getId() : asset.getName();
        this.efficiency = conversion.getEfficiency() == 0.0 ? Commons.DEFAULT_EFFICIENCY : conversion.getEfficiency();
        this.power = conversion.getPower();
        this.costInformation = conversion.getCostInformation();
        if (costInformation != null) {
            marginalCostProfile = costInformation.getMarginalCosts();
        }

        this.controlStrategy = conversion.getControlStrategy();
        for (Port port : conversion.getPort()) {
            if (port instanceof InPort) {
                inputPort = (InPort) port;
                inputCarrier = inputPort.getCarrier();
                costProfile = inputCarrier.getCost();
            } else {
                outputPort = (OutPort) port;
                outputCarrier = outputPort.getCarrier();
            }
        }

        specialConversion = inputCarrier.equals(outputCarrier);
    }

    @Override
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        super.createBidCurve(timeStep, now, marketInfo);
        // Generic Conversion or Gas Heater (All single-input/single-output conversions)

        // Checks if an asset is operational (accounts for Commissioning and
        // Decommissioning date)
        if (!isOperational(now)) {
            makeInflexibleConsumptionFunction(0);
            return;
        }
        if (controlStrategy instanceof DrivenByProfile) {
            drivenByProfile(timeStep, now);
        } else {
            if (getRole().equals(Role.PRODUCER)) {
                // PRODUCER
                if (controlStrategy instanceof DrivenByDemand) {
                    producerDrivenByDemand(timeStep, now);
                } else if (controlStrategy instanceof DrivenBySupply) {
                    producerDrivenBySupply(timeStep, now);
                }
            } else {
                // CONSUMER
                if (controlStrategy instanceof DrivenByDemand) {
                    // CONSUMER + DRIVENBYDEMAND
                    consumerDrivenByDemand(timeStep, now);
                } else if (controlStrategy instanceof DrivenBySupply) {
                    // CONSUMER + DRIVENBYSUPPLY
                    consumerDrivenBySupply(timeStep, now);
                }
            }
        }
    }

    private void drivenByProfile(long timeStep, Horizon now) {
        DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
        GenericProfile convProfile = drivenByProfile.getProfile();
        double energyOutput;
        if (convProfile != null) {
            if (Commons.isPowerProfile(convProfile)) {
                energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
            } else if (Commons.isEnergyProfile(convProfile)) {
                energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
            } else {
                throw new IllegalStateException(
                        "Profile in the outPort of " + conversionName + " is neither Power nor Energy!");
            }

            double eff = 1.0;
            if ((getRole() == Role.CONSUMER && drivenByProfile.getPort() instanceof OutPort) ||
                    (getRole() == Role.PRODUCER && drivenByProfile.getPort() instanceof InPort)) {
                eff = efficiency;
            }
            makeInflexibleConsumptionFunction(energyOutput / eff);
        } else {
            // Impossible!
            throw new IllegalStateException(conversionName + ": DrivenByProfile control strategy, but profile of driving port is null!");
        }
    }


    private boolean marketPricesAllowBid(Horizon now) {
        GenericProfile inCarrierCostProfile = inputCarrier.getCost();
        GenericProfile outCarrierCostProfile = outputCarrier.getCost();
        if (inCarrierCostProfile == null && outCarrierCostProfile == null) {
            return true;
        }
        Double outCarrierCost;
        Double inCarrierCost;
        if (inCarrierCostProfile != null) {
            inCarrierCost = Commons.aggregateCost(Commons.readProfile(inCarrierCostProfile, now));
        } else {
            inCarrierCost = getCost();
        }
        if (outCarrierCostProfile != null) {
            // If we have a price on the input carrier, but we don't have a price on the
            // output carrier, we will use the marginal cost as price for comparison
            outCarrierCost = Commons.aggregateCost(Commons.readProfile(outCarrierCostProfile, now));
        } else {
            outCarrierCost = getCost();
        }
        log.debug("{}: OutCarrierCost = {}, InCarrierCost = {}, efficiency = {}", getConversionName(),
                outCarrierCost, inCarrierCost, efficiency);
        if (outCarrierCost < inCarrierCost / efficiency) {
            // It is too expensive to consume
            log.debug("{}: generating flat bidcurve ", getConversionName());
            return false;
        } else {
            log.debug("{}: generating normal bidcurve ", getConversionName());

        }
        return true;
    }

    private void consumerDrivenByDemand(long timeStep, Horizon now) {
        DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
        Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
        if (!drivingCarrier.equals(carrier) || specialConversion) {
            GenericProfile convProfile;
            double energyOutput;
            if (inputPort != null) {
                convProfile = Commons.getEnergyProfile(inputPort);
            } else {
                // Impossible!
                throw new IllegalStateException(
                        "Conversion " + conversionName + " should not exist in this network!");
            }

            if (convProfile != null) {
                if (Commons.isPowerProfile(convProfile)) {
                    energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
                } else if (Commons.isEnergyProfile(convProfile)) {
                    energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
                } else {
                    throw new IllegalStateException(
                            "Profile in the outPort of " + conversionName + " is neither Power nor Energy!");
                }
                makeInflexibleConsumptionFunction(energyOutput);
            } else {
                throw new IllegalStateException("Profile in the outPort of " + conversionName + " is null!");
            }
        }
    }

    private void producerDrivenBySupply(long timeStep, Horizon now) {
        DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
        Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
        if (!drivingCarrier.equals(carrier) || specialConversion) {
            // PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
            GenericProfile convProfile;
            double energyOutput;
            if (outputPort != null) {
                convProfile = Commons.getEnergyProfile(outputPort);
            } else {
                // Impossible!
                throw new IllegalStateException(
                        "Conversion " + conversionName + " should not exist in this network!");
            }

            if (convProfile != null) {
                if (Commons.isPowerProfile(convProfile)) {
                    energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
                } else if (Commons.isEnergyProfile(convProfile)) {
                    energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
                } else {
                    throw new IllegalStateException(
                            "Profile in the outPort of " + conversionName + " is neither Power nor Energy!");
                }
                makeInflexibleProductionFunction(energyOutput);
            } else {
                throw new IllegalStateException("Profile in the outPort of " + conversionName + " is null!");
            }
        }
    }

    private void producerDrivenByDemand(long timeStep, Horizon now) {
        // PRODUCER + DRIVENBYDEMAND
        DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
        Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
        if (drivingCarrier.equals(carrier)) {
            // PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
            // Make Flexible Producer Curve
            double energyOutput = timeStep * power;
            updateCost(now);
            if (marketPricesAllowBid(now)) {
                makeAdjustableProductionFunction(energyOutput);
            } else {
                makeInflexibleConsumptionFunction(0);
            }
        } else {
            // PRODUCER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
            // = Impossible!
            throw new IllegalStateException(conversion.getName() + " cannot be a producer of " + carrier.getName());
        }
    }

    private void consumerDrivenBySupply(long timeStep, Horizon now) {
        DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
        Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
        if (drivingCarrier.equals(carrier)) {
            // CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
            // Make flexible consumption curve
            double energyOutput = (timeStep * power) / efficiency;
            updateCost(now);
            if (marketPricesAllowBid(now)) {
                makeAdjustableConsumptionFunction(energyOutput);
            } else {
                makeInflexibleConsumptionFunction(0);
            }
        } else {
            // CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
            // = Impossible!
            throw new IllegalStateException(conversionName + " cannot be a consumer of " + carrier.getName());
        }
    }

    private void updateCost(Horizon now) {
        Double aggregateCost = Double.NaN;
        if (marginalCostProfile != null) {
            aggregateCost = Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now));
        } else if (costProfile != null) {
            aggregateCost = Commons.aggregateCost(Commons.readProfile(costProfile, now)) / efficiency;
        } else {
            log.warn("Conversion {} is missing cost information! Defaulting to {}", conversionName,
                    DEFAULT_MARGINAL_COST);
        }
        setCost(Double.isNaN(aggregateCost) ? DEFAULT_MARGINAL_COST : aggregateCost);
    }

    @Override
    public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
        builder.tag("capability", "Conversion");

        // Generic Conversion or Gas Heater (All single-input/single-output conversions)
        if (getRole().equals(Role.PRODUCER)) {
            EmissionManager.getInstance(simulationId).addProducer(networkId, conversion, Math.abs(energy));
            // PRODUCER
            if (controlStrategy instanceof DrivenByDemand) {
                // PRODUCER + DRIVENBYDEMAND
                DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
                Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
                if (drivingCarrier.equals(carrier)) {
                    // PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
                    // = Calculate InputEnergy and write to InPort
                    double inputEnergy = -energy / efficiency;
                    Commons.writeProfile(inputPort, timestamp, inputEnergy);

                    if (inputCarrier instanceof EnergyCarrier) {
                        EnergyCarrier inputEnergyCarrier = (EnergyCarrier) inputCarrier;

                        double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                                inputEnergyCarrier.getEnergyContentUnit());
                        double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                                inputEnergyCarrier.getEmissionUnit());

                        if (carrierEnergyContent > Commons.eps) {
                            double inputCarrierQuantity = Math.abs(inputEnergy) / carrierEnergyContent;
                            double emission = inputCarrierQuantity * carrierEmission;
                            builder.value("emission", emission);
                            builder.value("fuelConsumption", inputCarrierQuantity);

                            double currentInputCarrierCost = Commons
                                    .aggregateCost(Commons.readProfile(inputCarrier.getCost(),
                                            new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
                            if (!Double.isNaN(currentInputCarrierCost)) {
                                double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                                builder.value("cost", inputCarrierCost);
                            }
                        }
                    }
                } else {
                    // PRODUCER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
                    // = Impossible!
                    throw new IllegalStateException(
                            conversion.getName() + " cannot be a producer of " + carrier.getName());
                }
            } else if (controlStrategy instanceof DrivenBySupply) {
                DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
                Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
                if (drivingCarrier.equals(carrier)) {
                    // PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
                    if (!specialConversion) {
                        // This is NOT a conversion with input and output carriers the same (in which
                        // case, do nothing).
                        // So Impossible!
                        throw new IllegalStateException(
                                conversion.getName() + " cannot be a producer of " + carrier.getName());
                    }
                } else {
                    // PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
                    // = Do Nothing
                }
            } else if (controlStrategy instanceof DrivenByProfile) {
                // PRODUCER + DRIVENBYPROFILE
                // = Calculate InputEnergy and write to InPort
                double inputEnergy = -energy / efficiency;
                Commons.writeProfile(inputPort, timestamp, inputEnergy);
                if (inputCarrier instanceof EnergyCarrier) {
                    EnergyCarrier inputEnergyCarrier = (EnergyCarrier) inputCarrier;

                    double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                            inputEnergyCarrier.getEnergyContentUnit());
                    double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                            inputEnergyCarrier.getEmissionUnit());

                    if (carrierEnergyContent > Commons.eps) {
                        double inputCarrierQuantity = Math.abs(inputEnergy) / carrierEnergyContent;
                        double emission = inputCarrierQuantity * carrierEmission;
                        builder.value("emission", emission);
                        builder.value("fuelConsumption", inputCarrierQuantity);

                        double currentInputCarrierCost = Commons
                                .aggregateCost(Commons.readProfile(inputCarrier.getCost(),
                                        new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
                        if (!Double.isNaN(currentInputCarrierCost)) {
                            double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                            builder.value("cost", inputCarrierCost);
                        }
                    }
                }
            }
        } else {
            // CONSUMER
            EmissionManager.getInstance(simulationId).addConsumer(networkId, conversion, Math.abs(energy));
            if (controlStrategy instanceof DrivenByDemand) {
                // CONSUMER + DRIVENBYDEMAND
                DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
                Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
                if (drivingCarrier.equals(carrier)) {
                    // CONSUMER + DRIVENBYDEMAND + DRIVINGCARRIER
                    // Impossible (iff it is not a special conversion)!
                    if (!specialConversion) {
                        throw new IllegalStateException(
                                conversion.getName() + " cannot be a consumer of " + carrier.getName());
                    }
                } else {
                    // CONSUMER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
                    // Do Nothing
                }
            } else if (controlStrategy instanceof DrivenBySupply) {
                // CONSUMER + DRIVENBYSUPPLY
                DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
                Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
                if (drivingCarrier.equals(carrier)) {
                    // CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
                    // = Calculate OutputEnergy and write to OutPort
                    double outputEnergy = energy * efficiency;
                    for (Port port : conversion.getPort()) {
                        if (port instanceof OutPort) {
                            Commons.writeProfile(port, timestamp, outputEnergy);
                        }
                    }
                    if (carrier instanceof EnergyCarrier) {
                        EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

                        double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                                inputEnergyCarrier.getEnergyContentUnit());
                        double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                                inputEnergyCarrier.getEmissionUnit());

                        if (carrierEnergyContent > Commons.eps) {
                            double inputCarrierQuantity = Math.abs(energy) / carrierEnergyContent;
                            double emission = inputCarrierQuantity * carrierEmission;
                            builder.value("emission", emission);
                            builder.value("fuelConsumption", inputCarrierQuantity);

                            double currentInputCarrierCost = Commons
                                    .aggregateCost(Commons.readProfile(inputCarrier.getCost(),
                                            new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
                            if (!Double.isNaN(currentInputCarrierCost)) {
                                double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                                builder.value("cost", inputCarrierCost);
                            }
                        }
                    }
                } else {
                    // CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
                    // = Impossible!
                    throw new IllegalStateException(
                            conversion.getName() + " cannot be a consumer of " + carrier.getName());
                }
            } else {
                // CONSUMER + DRIVENBYPROFILE
                // = Calculate OutputEnergy and write to OutPort
                double outputEnergy = energy * efficiency;
                Commons.writeProfile(outputPort, timestamp, outputEnergy);

                if (carrier instanceof EnergyCarrier) {
                    EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

                    double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                            inputEnergyCarrier.getEnergyContentUnit());
                    double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                            inputEnergyCarrier.getEmissionUnit());

                    if (carrierEnergyContent > Commons.eps) {
                        double inputCarrierQuantity = Math.abs(energy) / carrierEnergyContent;
                        double emission = inputCarrierQuantity * carrierEmission;
                        builder.value("emission", emission);
                        builder.value("fuelConsumption", inputCarrierQuantity);

                        double currentInputCarrierCost = Commons
                                .aggregateCost(Commons.readProfile(inputCarrier.getCost(),
                                        new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
                        if (!Double.isNaN(currentInputCarrierCost)) {
                            double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                            builder.value("cost", inputCarrierCost);
                        }
                    }
                }
            }
        }
    }

    public void setPriceInformation(Port port, List<PowerPricePair> priceInfo) {
        if (port.equals(inputPort)) {
            inputPrices = priceInfo;
            log.debug("Adding input prices to {}", this);
        } else if (port.equals(outputPort)) {
            outputPrices = priceInfo;
            log.debug("Adding output prices to {}", this);
        } else {
            log.warn("Set prices called on {} but port {} is not a port of {}", this, port, this);
        }
    }

}
