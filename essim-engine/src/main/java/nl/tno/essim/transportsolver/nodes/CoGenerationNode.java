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

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import esdl.Carrier;
import esdl.CoGeneration;
import esdl.ControlStrategy;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.DrivenBySupply;
import esdl.EnergyAsset;
import esdl.EnergyCarrier;
import esdl.GenericProfile;
import esdl.HeatCommodity;
import esdl.InPort;
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
public class CoGenerationNode extends AbstractBasicConversionNode {

	private CoGeneration coGenerationPlant;
	private InPort inputPort;
	private ControlStrategy controlStrategy;
	private String coGenName;
	private HashMap<Carrier, OutPort> outputCarriers;
	private double electricalEfficiency;
	private double heatEfficiency;
	private GenericProfile costProfile;
	private GenericProfile marginalCostProfile;

	@Builder(builderMethodName = "coGenerationNodeBuilder")
	public CoGenerationNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
			Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
				energy, cost, parent, carrier, children, timeStep, now, connectedPort);
		coGenerationPlant = (CoGeneration) asset;
		controlStrategy = coGenerationPlant.getControlStrategy();
		coGenName = (coGenerationPlant.getName() == null ? coGenerationPlant.getId() : coGenerationPlant.getName());
		power = coGenerationPlant.getPower();
		costInformation = coGenerationPlant.getCostInformation();
		if (costInformation != null) {
			marginalCostProfile = costInformation.getMarginalCosts();
		}

		heatEfficiency = coGenerationPlant.getHeatEfficiency();
		if (heatEfficiency < Commons.eps) {
			heatEfficiency = Commons.DEFAULT_COGEN_H_EFF;
			log.warn("Conversion asset {} is missing heat efficiency! Defaulting to 0.35 (35%)", coGenName);
		}
		electricalEfficiency = coGenerationPlant.getElectricalEfficiency();
		if (electricalEfficiency < Commons.eps) {
			electricalEfficiency = Commons.DEFAULT_COGEN_E_EFF;
			log.warn("Conversion asset {} is missing electrical efficiency! Defaulting to 0.55 (55%)", coGenName);
		}

		outputCarriers = new HashMap<Carrier, OutPort>();
		for (Port port : coGenerationPlant.getPort()) {
			if (port instanceof OutPort) {
				if (!outputCarriers.containsKey(port.getCarrier())) {
					outputCarriers.put(port.getCarrier(), (OutPort) port);
				}
			} else {
				inputPort = (InPort) port;
				costProfile = port.getCarrier().getCost();
			}
		}

	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}
		if (controlStrategy == null) {
			log.warn("Control Strategy of " + coGenName + " is not defined! It will not participate in this round!");
			makeInflexibleProductionFunction(0.0);
		} else {
			if (getRole().equals(Role.PRODUCER)) {
				// PRODUCER
				if (controlStrategy instanceof DrivenByDemand) {
					// PRODUCER + DRIVENBYDEMAND
					DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
					Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
					if (drivingCarrier.equals(carrier)) {
						// PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
						// = Make flexible Producer Curve
						double heatFactor = 1.0;
						double costFactor = electricalEfficiency;
						if (carrier instanceof HeatCommodity) {
							heatFactor = heatEfficiency / electricalEfficiency;
							costFactor = heatEfficiency;
						}
						double energyOutput = timeStep * power * heatFactor;
						if (marginalCostProfile != null) {
							Double aggregateCost = Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now));
							if(Double.isNaN(aggregateCost)) {
								aggregateCost = DEFAULT_MARGINAL_COST;
							}
						} else if (costProfile != null) {
							Double aggregateCost = Commons.aggregateCost(Commons.readProfile(costProfile, now))/ costFactor;
							if(Double.isNaN(aggregateCost)) {
								aggregateCost = DEFAULT_MARGINAL_COST;
							}
							setCost(aggregateCost);
						} else {
							log.warn("CoGeneration {} is missing cost information! Defaulting to {}", coGenName,
									DEFAULT_MARGINAL_COST);
							setCost(DEFAULT_MARGINAL_COST);
						}
						makeAdjustableProductionFunction(energyOutput);
					} else {
						if (outputCarriers.containsKey(carrier)) {
							// PRODUCER + DRIVENBYDEMAND + OTHER OUTPUT CARRIER
							double energyOutput = Double.NaN;
							GenericProfile convProfile = Commons.getEnergyProfile(outputCarriers.get(carrier));

							if (convProfile != null) {
								if (Commons.isPowerProfile(convProfile)) {
									energyOutput = timeStep
											* Commons.aggregatePower(Commons.readProfile(convProfile, now));
								} else if (Commons.isEnergyProfile(convProfile)) {
									energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
								} else {
									throw new IllegalStateException(
											"Profile in the outPort of " + coGenName + " is neither Power nor Energy!");
								}
								makeInflexibleProductionFunction(energyOutput);
							} else {
								throw new IllegalStateException("Profile in the outPort of " + coGenName + " is null!");
							}
						} else {
							// PRODUCER + DRIVENBYDEMAND + ANY OTHER CARRIER
							// = Impossible!
							throw new IllegalStateException(
									"Conversion " + coGenName + " cannot be a PRODUCER in this network!");
						}
					}
				} else if (controlStrategy instanceof DrivenBySupply) {
					// PRODUCER + DRIVENBYSUPPLY
					DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
					Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
					if (drivingCarrier.equals(carrier)) {
						// PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
						// = Impossible!
						throw new IllegalStateException(
								"Conversion " + coGenName + " cannot be a PRODUCER in this network!");
					} else {
						// PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
						// = Read from OutPort, make flat production curve
						double energyOutput = Double.NaN;
						OutPort outPort = outputCarriers.get(carrier);
						GenericProfile convProfile;
						if (outPort != null) {
							convProfile = Commons.getEnergyProfile(outPort);
						} else {
							// Impossible!
							throw new IllegalStateException(
									"Conversion " + coGenName + " should not exist in this network!");
						}

						if (convProfile != null) {
							if (Commons.isPowerProfile(convProfile)) {
								energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
							} else if (Commons.isEnergyProfile(convProfile)) {
								energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
							} else {
								throw new IllegalStateException("Profile in the outPort of " + coGenerationPlant
										+ " is neither Power nor Energy!");
							}
							makeInflexibleProductionFunction(energyOutput);
						} else {
							throw new IllegalStateException(
									"Profile in the outPort of " + coGenerationPlant + " is null!");
						}
					}
				} else {
					// PRODUCER + DRIVENBYPROFILE
					// = Read from Profile, make flat production curve
					DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
					GenericProfile convProfile = drivenByProfile.getProfile();
					double energyOutput = Double.NaN;

					if (convProfile != null) {
						if (Commons.isPowerProfile(convProfile)) {
							energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
						} else if (Commons.isEnergyProfile(convProfile)) {
							energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
						} else {
							throw new IllegalStateException("Profile attached to " + coGenerationPlant
									+ "'s DrivenByProfile strategy is neither Power nor Energy!");
						}
						makeInflexibleProductionFunction(energyOutput);
					} else {
						throw new IllegalStateException(
								"Profile attached to " + coGenerationPlant + "'s DrivenByProfile strategy is null!");
					}
				}
			} else {
				// CONSUMER
				if (controlStrategy instanceof DrivenByDemand) {
					// CONSUMER + DRIVENBYDEMAND
					if (outputCarriers.containsKey(carrier)) {
						// CONSUMER + DRIVENBYDEMAND + ONE OF OUTPORT CARRIERS
						// Impossible!
						throw new IllegalStateException(
								"Conversion " + coGenName + " cannot be a CONSUMER in this network!");
					} else {
						// CONSUMER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
						// = Read from InPort, make flat consumption curve
						GenericProfile convProfile = Commons.getEnergyProfile(inputPort);
						double energyOutput = Double.NaN;

						if (convProfile != null) {
							if (Commons.isPowerProfile(convProfile)) {
								energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
							} else if (Commons.isEnergyProfile(convProfile)) {
								energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
							} else {
								throw new IllegalStateException("Profile in the inPort of " + coGenerationPlant
										+ " is neither Power nor Energy!");
							}
							makeInflexibleConsumptionFunction(energyOutput);
						} else {
							throw new IllegalStateException(
									"Profile in the outPort of " + coGenerationPlant + " is null!");
						}
					}
				} else if (controlStrategy instanceof DrivenBySupply) {
					// CONSUMER + DRIVENBYSUPPLY
					DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
					Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
					if (drivingCarrier.equals(carrier)) {
						// CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
						// = Make Flexible Consumer curve
						double energyInput = (timeStep * power) / electricalEfficiency;
						if (marginalCostProfile != null) {
							Double aggregateCost = Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now));
							if(Double.isNaN(aggregateCost)) {
								aggregateCost = DEFAULT_MARGINAL_COST;
							}
						} else if (costProfile != null) {
							Double aggregateCost = Commons.aggregateCost(Commons.readProfile(costProfile, now))* (electricalEfficiency + heatEfficiency);
							if(Double.isNaN(aggregateCost)) {
								aggregateCost = DEFAULT_MARGINAL_COST;
							}
							setCost(aggregateCost);
						} else {
							log.warn("CoGeneration {} is missing cost information! Defaulting to {}", coGenName,
									DEFAULT_MARGINAL_COST);
							setCost(DEFAULT_MARGINAL_COST);
						}
						makeAdjustableConsumptionFunction(energyInput);
					} else {
						// PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
						// = Impossible!
						throw new IllegalStateException(
								"Conversion " + coGenName + " cannot be a CONSUMER in this network!");
					}
				} else {
					// CONSUMER + DRIVENBYPROFILE
					// = Read from Profile, make flat consumption curve
					DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
					GenericProfile convProfile = drivenByProfile.getProfile();
					double energyOutput = Double.NaN;

					if (convProfile != null) {
						if (Commons.isPowerProfile(convProfile)) {
							energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
						} else if (Commons.isEnergyProfile(convProfile)) {
							energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
						} else {
							throw new IllegalStateException("Profile attached to " + coGenName
									+ "'s DrivenByProfile strategy is neither Power nor Energy!");
						}
						makeInflexibleConsumptionFunction(energyOutput / efficiency);
					} else {
						throw new IllegalStateException(
								"Profile attached to " + coGenName + "'s DrivenByProfile strategy is null!");
					}
				}
			}
		}

	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Conversion");

		if (getRole().equals(Role.PRODUCER)) {
			EmissionManager.getInstance(simulationId).addProducer(networkId, coGenerationPlant, Math.abs(energy));
			// PRODUCER
			if (controlStrategy instanceof DrivenByDemand) {
				// PRODUCER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
					// = Calculate InputEnergy and write to InPort, calculate other OutputEnergy and
					// write
					// to other OutPort
					double inputEfficiency = 1.0;
					double otherFraction = 1.0;
					if (carrier instanceof HeatCommodity) {
						inputEfficiency = heatEfficiency;
						otherFraction = electricalEfficiency / heatEfficiency;
					} else {
						inputEfficiency = electricalEfficiency;
						otherFraction = heatEfficiency / electricalEfficiency;
					}

					double inputEnergy = -energy / inputEfficiency;
					double otherOutputEnergy = -energy * otherFraction;
					OutPort otherOutputPort = null;
					for (Port port : coGenerationPlant.getPort()) {
						if (port instanceof OutPort) {
							if (!port.getCarrier().equals(carrier)) {
								otherOutputPort = (OutPort) port;
								break;
							}
						}
					}
					Commons.writeProfile(inputPort, timestamp, inputEnergy);
					Commons.writeProfile(otherOutputPort, timestamp, otherOutputEnergy);

					Carrier inputCarrier = inputPort.getCarrier();
					if (inputCarrier != null) {
						if (inputCarrier instanceof EnergyCarrier) {
							EnergyCarrier inputEnergyCarrier = (EnergyCarrier) inputCarrier;

							double carrierEnergyContent = Commons.toStandardizedUnits(
									inputEnergyCarrier.getEnergyContent(), inputEnergyCarrier.getEnergyContentUnit());
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
					double inputEfficiency = 1.0;
					if (carrier instanceof HeatCommodity) {
						inputEfficiency = heatEfficiency;
					} else {
						inputEfficiency = electricalEfficiency;
					}

					double inputEnergy = -energy / inputEfficiency;

					Carrier inputCarrier = inputPort.getCarrier();
					if (inputCarrier != null) {
						if (inputCarrier instanceof EnergyCarrier) {
							EnergyCarrier inputEnergyCarrier = (EnergyCarrier) inputCarrier;

							double carrierEnergyContent = Commons.toStandardizedUnits(
									inputEnergyCarrier.getEnergyContent(), inputEnergyCarrier.getEnergyContentUnit());
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
				}
			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// = Calculate InputEnergy and write to InPort
				double inputEfficiency = 1.0;
				if (carrier instanceof HeatCommodity) {
					inputEfficiency = heatEfficiency;
				} else {
					inputEfficiency = electricalEfficiency;
				}

				double inputEnergy = -energy / inputEfficiency;
				Commons.writeProfile(inputPort, timestamp, inputEnergy);

				Carrier inputCarrier = inputPort.getCarrier();
				if (inputCarrier != null) {
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
			}
		} else {
			EmissionManager.getInstance(simulationId).addConsumer(networkId, coGenerationPlant, Math.abs(energy));
			// CONSUMER
			if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// = Calculate 2x OutputEnergy and write to OutPorts
					double heatOutputEnergy = energy * heatEfficiency;
					double electricityOutputEnergy = energy * electricalEfficiency;
					for (Entry<Carrier, OutPort> e : outputCarriers.entrySet()) {
						if (e.getKey() instanceof HeatCommodity) {
							Commons.writeProfile(e.getValue(), timestamp, heatOutputEnergy);
						} else {
							Commons.writeProfile(e.getValue(), timestamp, electricityOutputEnergy);
						}
					}
				}
			} else if (controlStrategy instanceof DrivenByProfile) {
				// CONSUMER + DRIVENBYPROFILE
				// = Calculate 2x OutputEnergy and write to OutPorts
				double heatOutputEnergy = energy * heatEfficiency;
				double electricityOutputEnergy = energy * electricalEfficiency;
				for (Entry<Carrier, OutPort> e : outputCarriers.entrySet()) {
					if (e.getKey() instanceof HeatCommodity) {
						Commons.writeProfile(e.getValue(), timestamp, heatOutputEnergy);
					} else {
						Commons.writeProfile(e.getValue(), timestamp, electricityOutputEnergy);
					}
				}
			}
		}
	}
}
