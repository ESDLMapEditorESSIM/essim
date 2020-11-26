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
import esdl.ControlStrategy;
import esdl.Conversion;
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
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ConversionNode extends Node {

	protected static final double DEFAULT_CONVERSION_EFFICIENCY = 0.6;
	protected Conversion conversion;
	protected double efficiency;
	protected double power;
	protected CostInformation costInformation;
	protected ControlStrategy controlStrategy;
	private InPort inputPort;
	private Carrier inputCarrier;
	private OutPort outputPort;
	private Carrier outputCarrier;
	private boolean specialConversion;
	private String conversionName;

	@Builder(builderMethodName = "conversionNodeBuilder")
	public ConversionNode(String simulationId, String nodeId, String address, String networkId,
			JSONArray animationArray, JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.conversion = (Conversion) asset;
		this.conversionName = asset.getName() == null ? asset.getId() : asset.getName();
		this.efficiency = conversion.getEfficiency() == 0.0 ? Commons.DEFAULT_EFFICIENCY : conversion.getEfficiency();
		this.power = conversion.getPower();
		this.costInformation = conversion.getCostInformation();
		this.controlStrategy = conversion.getControlStrategy();
		for (Port port : conversion.getPort()) {
			if (port instanceof InPort) {
				inputPort = (InPort) port;
				inputCarrier = inputPort.getCarrier();
			} else {
				outputPort = (OutPort) port;
				outputCarrier = outputPort.getCarrier();
			}
		}

		specialConversion = inputCarrier.equals(outputCarrier);
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		// Generic Conversion or Gas Heater (All single-input/single-output conversions)

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}

		if (getRole().equals(Role.PRODUCER)) {
			// PRODUCER
			if (controlStrategy instanceof DrivenByDemand) {
				// PRODUCER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
					// Make Flexible Producer Curve
					double energyOutput = timeStep * power;
					if (costInformation == null) {
						log.warn("Conversion {} is missing cost information!", conversionName);
						setCost(DEFAULT_MARGINAL_COST);
					} else {
						GenericProfile marginalCosts = costInformation.getMarginalCosts();
						if (marginalCosts != null) {
							setCost(Commons.aggregateCost(Commons.readProfile(marginalCosts, now)));
						}
					}
					makeAdjustableProductionFunction(energyOutput);
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
					if (specialConversion) {
						// This is a conversion with input and output carriers the same.
						// Read from outport. Make a flat bid producer curve.
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
								throw new IllegalStateException("Profile in the outPort of " + conversionName
										+ " is neither Power nor Energy!");
							}
							makeInflexibleProductionFunction(energyOutput);
						} else {
							throw new IllegalStateException(
									"Profile in the outPort of " + conversionName + " is null!");
						}
					} else {
						// Impossible!
						throw new IllegalStateException(
								conversionName + " cannot be a producer of " + carrier.getName());
					}
				} else {
					// PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// Read from OutPort, make flat production curve
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
			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// Read from Profile, make flat production curve
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
					if(drivenByProfile.getPort() instanceof InPort) {
						eff = efficiency;
					}
					makeInflexibleProductionFunction(eff * energyOutput);
				} else {
					// Impossible!
					throw new IllegalStateException("Profile in the outPort of " + conversionName + " is null!");
				}
			}
		} else {
			// CONSUMER
			if (controlStrategy instanceof DrivenByDemand) {
				// CONSUMER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort().getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYDEMAND + DRIVINGCARRIER
					if (specialConversion) {
						// This is a conversion with input and output carriers the same.
						// Read from outport. Make a flat bid consumer curve.
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
								throw new IllegalStateException("Profile in the outPort of " + conversionName
										+ " is neither Power nor Energy!");
							}
							makeInflexibleConsumptionFunction(energyOutput);
						} else {
							throw new IllegalStateException(
									"Profile in the outPort of " + conversionName + " is null!");
						}
					} else {
						// Impossible!
						throw new IllegalStateException(
								conversionName + " cannot be a consumer of " + carrier.getName());
					}
				} else {
					// CONSUMER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
					// Read from InPort, make flat consumption curve
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
			} else if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort().getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// Make flexible consumption curve
					double energyOutput = (timeStep * power) / efficiency;
					if (costInformation == null) {
						log.warn("Conversion {} is missing cost information!", conversionName);
						setCost(DEFAULT_MARGINAL_COST);
					} else {
						GenericProfile marginalCosts = costInformation.getMarginalCosts();
						if (marginalCosts != null) {
							setCost(Commons.aggregateCost(Commons.readProfile(marginalCosts, now)));
						}
					}
					makeAdjustableConsumptionFunction(energyOutput);
				} else {
					// CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// = Impossible!
					throw new IllegalStateException(conversionName + " cannot be a consumer of " + carrier.getName());
				}
			} else {
				// CONSUMER + DRIVENBYPROFILE
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
					if(drivenByProfile.getPort() instanceof OutPort) {
						eff = efficiency;
					}
					makeInflexibleConsumptionFunction(energyOutput / eff);
				} else {
					// Impossible!
					throw new IllegalStateException("Profile in the outPort of " + conversionName + " is null!");
				}
			}
		}

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

						double inputCarrierQuantity = Math.abs(inputEnergy) / carrierEnergyContent;
						double currentInputCarrierCost = Commons
								.aggregateCost(Commons.readProfile(inputCarrier.getCost(),
										new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
						double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
						double emission = inputCarrierQuantity * carrierEmission;

						builder.value("emission", emission);
						builder.value("fuelConsumption", inputCarrierQuantity);
						builder.value("cost", inputCarrierCost);
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

					double inputCarrierQuantity = Math.abs(inputEnergy) / carrierEnergyContent;
					double currentInputCarrierCost = Commons.aggregateCost(Commons.readProfile(inputCarrier.getCost(),
							new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
					double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
					double emission = inputCarrierQuantity * carrierEmission;

					builder.value("emission", emission);
					builder.value("fuelConsumption", inputCarrierQuantity);
					builder.value("cost", inputCarrierCost);
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

						double inputCarrierQuantity = Math.abs(energy) / carrierEnergyContent;
						double currentInputCarrierCost = Commons.aggregateCost(Commons.readProfile(carrier.getCost(),
								new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
						double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
						double emission = inputCarrierQuantity * carrierEmission;

						builder.value("emission", emission);
						builder.value("fuelConsumption", inputCarrierQuantity);
						builder.value("cost", inputCarrierCost);
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

					double inputCarrierQuantity = Math.abs(energy) / carrierEnergyContent;
					double currentInputCarrierCost = Commons.aggregateCost(Commons.readProfile(carrier.getCost(),
							new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
					double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
					double emission = inputCarrierQuantity * carrierEmission;

					builder.value("emission", emission);
					builder.value("fuelConsumption", inputCarrierQuantity);
					builder.value("cost", inputCarrierCost);
				}
			}
		}
	}

}
