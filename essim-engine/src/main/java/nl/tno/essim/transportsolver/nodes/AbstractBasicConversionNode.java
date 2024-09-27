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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.common.util.EList;

import esdl.AbstractBasicConversion;
import esdl.AbstractBehaviour;
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
import esdl.InputOutputRelation;
import esdl.OutPort;
import esdl.Port;
import esdl.PortRelation;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.Market;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AbstractBasicConversionNode extends Node {

    protected static final double DEFAULT_CONVERSION_EFFICIENCY = 0.6;
    @Getter
    @Setter
    private static boolean disablePriceBasedBidDecisions = System.getenv("DISABLE_PRICE_BASED_BID_DECISIONS") != null;

    protected AbstractBasicConversion conversion;
    protected double power;
    protected CostInformation costInformation;
    protected ControlStrategy controlStrategy;
    private boolean specialConversion;
    private String conversionName;
    private GenericProfile marginalCostProfile;
    private HashMap<Port, GenericProfile> costProfileMap = new HashMap<>();
    private Port mainPort;
    private HashMap<Port, Double> ratioMap = new HashMap<>();

    @Builder(builderMethodName = "abstractBasicConversionNodeBuilder")
    public AbstractBasicConversionNode(String simulationId, String nodeId, String address, String networkId,
                                       EnergyAsset asset, int directionFactor, Role role, BidFunction demandFunction,
                                       double energy, double cost, Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now,
                                       Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        this.conversion = (AbstractBasicConversion) asset;
        this.conversionName = asset.getName() == null ? asset.getId() : asset.getName();
        this.power = conversion.getPower();
        this.costInformation = conversion.getCostInformation();
        if (costInformation != null) {
            marginalCostProfile = costInformation.getMarginalCosts();
        }
        this.controlStrategy = conversion.getControlStrategy();
        for (Port port : conversion.getPort()) {
//            if (port instanceof InPort) {
            GenericProfile portCost = port.getCarrier().getCost();
            if (portCost != null) {
                costProfileMap.put(port, portCost);
            }
//            }
        }

        EList<AbstractBehaviour> conversionBehaviours = conversion.getBehaviour();
        if (conversionBehaviours != null && !conversionBehaviours.isEmpty()) {
            for (AbstractBehaviour behaviour : conversionBehaviours) {
                if (behaviour instanceof InputOutputRelation) {
                    InputOutputRelation conversionTable = (InputOutputRelation) behaviour;
                    mainPort = conversionTable.getMainPort();
                    if (mainPort == null) {
                        throw new IllegalArgumentException(
                                "No main port defined in InputOutputRelation of asset " + conversionName + "!");
                    }
                    boolean mainPortFound = false;
                    for (Port port : conversion.getPort()) {
                        if (port.equals(mainPort)) {
                            mainPortFound = true;
                            break;
                        }
                    }
                    if (!mainPortFound) {
                        throw new IllegalArgumentException(
                                "MainPort defined in InputOutputRelation for conversion asset " + conversionName
                                        + " was not found in the asset!");
                    }
                    ratioMap.put(mainPort, 1.0);
                    if (conversionTable.getMainPortRelation().size() < conversion.getPort().size() - 1) {
                        throw new IllegalArgumentException("Not enough (" + conversionTable.getMainPortRelation().size()
                                + ") InputOutput relations defined for conversion asset " + conversionName + " with "
                                + conversion.getPort().size() + "!");
                    }
                    for (PortRelation portRelation : conversionTable.getMainPortRelation()) {
                        if (portRelation.getPort() == null) {
                            throw new IllegalArgumentException(
                                    "InputOutputRelation defined for asset " + conversionName + " has a missing port!");
                        }
                        if (portRelation.getRatio() == 0.0) {
                            throw new IllegalArgumentException("InputOutputRelation defined for asset " + conversionName
                                    + " has ratio 0.0 for port " + portRelation.getPort().getId() + "!");
                        }
                        ratioMap.put(portRelation.getPort(), portRelation.getRatio());
                    }
                }
            }
        } else {
            InPort inputPort = null;
            OutPort outputPort = null;
            for (Port port : conversion.getPort()) {
                if (port instanceof InPort) {
                    inputPort = (InPort) port;
                } else {
                    outputPort = (OutPort) port;
                }
            } // We need to put the efficiency in the ratio map
            double efficiency = conversion.getEfficiency() == 0.0 ? Commons.DEFAULT_EFFICIENCY
                    : conversion.getEfficiency();
            mainPort = outputPort;
            ratioMap.put(outputPort, 1.0);
            ratioMap.put(inputPort, 1 / efficiency);
            specialConversion = inputPort.getCarrier().equals(outputPort.getCarrier());
        }
    }

    @Override
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        updateMarketInfo(marketInfo);
        // Generic Conversion or Gas Heater (All single-input/single-output conversions)

        // Checks if an asset is operational (accounts for Commissioning and
        // Decommissioning date)
        if (!isOperational(now)) {
            makeInflexibleConsumptionFunction(0);
            return;
        }
        if (controlStrategy instanceof DrivenByProfile) {
            bidCurveDrivenByProfile(timeStep, now);
        } else {
            if (getRole().equals(Role.PRODUCER)) {
                // PRODUCER
                if (controlStrategy instanceof DrivenByDemand) {
                    producerBidCurveDrivenByDemand(timeStep, now);
                } else if (controlStrategy instanceof DrivenBySupply) {
                    producerBidCurveDrivenBySupply(timeStep, now);
                }
            } else {
                // CONSUMER
                if (controlStrategy instanceof DrivenByDemand) {
                    // CONSUMER + DRIVENBYDEMAND
                    consumerBidCurveDrivenByDemand(timeStep, now);
                } else if (controlStrategy instanceof DrivenBySupply) {
                    // CONSUMER + DRIVENBYSUPPLY
                    consumerBidCurveDrivenBySupply(timeStep, now);
                }
            }
        }
    }

    private void bidCurveDrivenByProfile(long timeStep, Horizon now) {
        DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
        GenericProfile convProfile = drivenByProfile.getProfile();
        Port drivenByProfilePort = drivenByProfile.getPort();
        if (drivenByProfilePort == null) {
            drivenByProfilePort = getConnectedPort();
        }
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

            double efficiency = ratioMap.get(getConnectedPort()) / ratioMap.get(drivenByProfilePort);

            makeInflexibleConsumptionFunction(energyOutput * efficiency);
        } else {
            // Impossible!
            throw new IllegalStateException(
                    conversionName + ": DrivenByProfile control strategy, but profile of driving port is null!");
        }
    }

    private boolean marketPricesAllowBid(Horizon now) {
        if (disablePriceBasedBidDecisions) {
            return true;
        }
        Set<Entry<Port, Double>> entries = ratioMap.entrySet();
        boolean allInAvailable = true;
        boolean allOutAvailable = true;
        double sumIn = 0;
        double sumOut = 0;
        for (Entry<Port, Double> entry : entries) {
            GenericProfile carrierCostProfile = entry.getKey().getCarrier().getCost();
            Double carrierCost;
            if (carrierCostProfile == null) {
                if (entry.getKey() instanceof InPort) {
                    allInAvailable = false;
                } else {
                    allOutAvailable = false;
                }
                carrierCost = getCost();
            } else {
                carrierCost = Commons.aggregateCost(Commons.readProfile(carrierCostProfile, now));
            }
            if (entry.getKey() instanceof InPort) {
                sumIn += entry.getValue() * carrierCost;
            } else {
                sumOut += entry.getValue() * carrierCost;
            }
        }
        if (!(allOutAvailable || allInAvailable)) {
//			log.debug("Not enough cost information for {}, using normal bidcurves", getConversionName());
            return true;
        }
        if (sumOut < sumIn) {
            // It is too expensive to convert
            log.trace("{}: generating flat bidcurve ", getConversionName());
            return false;
        } else {
            log.trace("{}: generating normal bidcurve ", getConversionName());

        }
        return true;
    }

    private void consumerBidCurveDrivenBySupply(long timeStep, Horizon now) {
        DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
        Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
        if (drivingCarrier.equals(carrier)) {
            // CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
            // Make flexible consumption curve
            double efficiency = ratioMap.get(drivenBySupply.getInPort()) / ratioMap.get(getMainPort());
            double portEnergy = (timeStep * power) * efficiency;
            updateCost(now);
            if (marketPricesAllowBid(now)) {
                makeAdjustableConsumptionFunction(portEnergy);
            } else {
                makeInflexibleConsumptionFunction(0);
            }
        } else {
            // CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
            GenericProfile convProfile = Commons.getEnergyProfile(getConnectedPort());
            makeInflexibleProductionFunction(convProfile, getConversionName(), timeStep, now);
        }
    }

    private void consumerBidCurveDrivenByDemand(long timeStep, Horizon now) {
        DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
        Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
        if (!drivingCarrier.equals(carrier) || specialConversion) {
            GenericProfile convProfile;
            double energyOutput;
            InPort inputPort = (InPort) getConnectedPort();
            if (inputPort != null) {
                convProfile = Commons.getEnergyProfile(inputPort);
            } else {
                // Impossible!
                throw new IllegalStateException("Conversion " + conversionName + " should not exist in this network!");
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

    private void producerBidCurveDrivenBySupply(long timeStep, Horizon now) {
        DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
        Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
        if (!drivingCarrier.equals(carrier) || specialConversion) {
            // PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
            GenericProfile convProfile;
            OutPort outputPort = (OutPort) getConnectedPort();
            if (outputPort != null) {
                convProfile = Commons.getEnergyProfile(outputPort);
            } else {
                // Impossible!
                throw new IllegalStateException("Conversion " + conversionName + " should not exist in this network!");
            }

            makeInflexibleProductionFunction(convProfile, conversionName, timeStep, now);
        }
    }

    private void producerBidCurveDrivenByDemand(long timeStep, Horizon now) {
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
            // PRODUCER + DRIVENBYDEMAND + OTHER OUTPUT CARRIER
            GenericProfile convProfile = Commons.getEnergyProfile(getConnectedPort());
            makeInflexibleProductionFunction(convProfile, getConversionName(), timeStep, now);
        }
    }

    protected void makeInflexibleProductionFunction(GenericProfile convProfile, String name, long timeStep,
                                                    Horizon now) {
        double energyOutput;
        if (convProfile != null) {
            if (Commons.isPowerProfile(convProfile)) {
                energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
            } else if (Commons.isEnergyProfile(convProfile)) {
                energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
            } else {
                throw new IllegalStateException("Profile in the outPort of " + name + " is neither Power nor Energy!");
            }
            makeInflexibleProductionFunction(
                    energyOutput);
        } else {
            throw new IllegalStateException("Profile in the outPort of " + name + " is null!");
        }
    }

    private void updateCost(Horizon now) {
        Double aggregateCost = Double.NaN;
        if (marginalCostProfile != null) {
            aggregateCost = Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now));
        } else if (!costProfileMap.isEmpty() && !ratioMap.isEmpty()) {
            double costSum = 0.0;
            for (Map.Entry<Port, GenericProfile> portCostPair : costProfileMap.entrySet()) {
                Port inPort = portCostPair.getKey();
                GenericProfile costProfile = portCostPair.getValue();
                Double cost = 0.0;
                double ratio = 1.0;
                if (costProfile != null) {
                    cost = Commons.aggregateCost(Commons.readProfile(costProfile, now));
                    if (inPort != null) {
                        ratio = ratioMap.get(connectedPort) / ratioMap.get(inPort);
                    }
                }
                costSum += cost / ratio;
            }
            if (costSum != 0.0) {
                aggregateCost = costSum;
            }
        }
        setCost(Double.isNaN(aggregateCost) ? getDefaultMarginalCost() : aggregateCost);
    }

    @Override
    public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
        builder.tag("capability", "Conversion");
        if (getRole().equals(Role.PRODUCER)) {
            EmissionManager.getInstance(simulationId).addProducer(networkId, conversion, Math.abs(energy));
        } else {
            EmissionManager.getInstance(simulationId).addConsumer(networkId, conversion, Math.abs(energy));
        }
        Port controlPort = null;

        if (controlStrategy instanceof DrivenByDemand) {
            controlPort = ((DrivenByDemand) controlStrategy).getOutPort();
        }
        if (controlStrategy instanceof DrivenBySupply) {
            controlPort = ((DrivenBySupply) controlStrategy).getInPort();
        }
        if (controlStrategy instanceof DrivenByProfile) {
            controlPort = ((DrivenByProfile) controlStrategy).getPort();
        }
        if (controlPort != null && controlPort.getCarrier().equals(carrier)) {
            updateAllocation(timestamp, builder, controlPort);
        }
    }

    private void updateAllocation(EssimTime timestamp, ObservationBuilder builder, Port controlPort) {
        for (Entry<Port, Double> entry : ratioMap.entrySet()) {
            if (!entry.getKey().equals(controlPort)) {
                double efficiency = ratioMap.get(controlPort) / entry.getValue();
                double portEnergy = energy / efficiency;
                if (getRole().equals(Role.PRODUCER)) {
                    portEnergy = -portEnergy;
                }
                Commons.writeProfile(entry.getKey(), timestamp, portEnergy);
                if (entry.getKey().getCarrier() instanceof EnergyCarrier) {
                    calculateCarrierProperties(timestamp, builder, portEnergy, entry.getKey().getCarrier());

                }
            }
        }
    }

    static void calculateCarrierProperties(EssimTime timestamp, ObservationBuilder builder, double inputEnergy,
                                           Carrier theCarrier) {
        EnergyCarrier inputEnergyCarrier = (EnergyCarrier) theCarrier;
        GenericProfile cost = theCarrier.getCost();

        double carrierEnergyContent = Commons.toStandardizedUnits(inputEnergyCarrier.getEnergyContent(),
                inputEnergyCarrier.getEnergyContentUnit());
        double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
                inputEnergyCarrier.getEmissionUnit());

        if (carrierEnergyContent > Commons.eps) {
            double inputCarrierQuantity = Math.abs(inputEnergy) / carrierEnergyContent;
            double emission = inputCarrierQuantity * carrierEmission;
            builder.value("emission", emission);
            builder.value("fuelConsumption", inputCarrierQuantity);

            double currentInputCarrierCost = Commons.aggregateCost(
                    Commons.readProfile(cost, new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
            if (!Double.isNaN(currentInputCarrierCost)) {
                double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
                builder.value("cost", inputCarrierCost);
            }
        }
    }

}
