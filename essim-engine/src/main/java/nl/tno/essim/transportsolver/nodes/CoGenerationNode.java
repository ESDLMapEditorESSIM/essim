package nl.tno.essim.transportsolver.nodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

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
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class CoGenerationNode extends ConversionNode {

	private CoGeneration coGenerationPlant;
	private InPort inputPort;
	private ControlStrategy controlStrategy;
	private String coGenName;
	private HashMap<Carrier, OutPort> outputCarriers;
	private double electricalEfficiency;
	private double heatEfficiency;

	@Builder(builderMethodName = "coGenerationNodeBuilder")
	public CoGenerationNode(String simulationId, String nodeId, String address, String networkId,
			JSONArray animationArray, JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		coGenerationPlant = (CoGeneration) asset;
		controlStrategy = coGenerationPlant.getControlStrategy();
		coGenName = (coGenerationPlant.getName() == null ? coGenerationPlant.getId() : coGenerationPlant.getName());
		power = coGenerationPlant.getPower();
		costInformation = coGenerationPlant.getCostInformation();
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
						if (carrier instanceof HeatCommodity) {
							heatFactor = heatEfficiency / electricalEfficiency;
						}
						double energyOutput = timeStep * power * heatFactor;
						if (costInformation == null) {
							log.warn("Conversion {} is missing cost information!", coGenName);
							setCost(0.5);
						} else {
							GenericProfile marginalCosts = costInformation.getMarginalCosts();
							if (marginalCosts != null) {
								setCost(Commons.aggregateCost(Commons.readProfile(marginalCosts, now)));
							}
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
								energyOutput = Commons.aggregatePower(Commons.readProfile(convProfile, now));
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
							energyOutput = Commons.aggregatePower(Commons.readProfile(convProfile, now));
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
								energyOutput = Commons.aggregatePower(Commons.readProfile(convProfile, now));
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
						double energyInput = (timeStep * power) / efficiency;
						if (costInformation == null) {
							log.warn("Conversion {} is missing cost information!", coGenName);
							setCost(0.5);
						} else {
							GenericProfile marginalCosts = costInformation.getMarginalCosts();
							if (marginalCosts != null) {
								setCost(Commons.aggregateCost(Commons.readProfile(marginalCosts, now)));
							}
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
							energyOutput = Commons.aggregatePower(Commons.readProfile(convProfile, now));
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
