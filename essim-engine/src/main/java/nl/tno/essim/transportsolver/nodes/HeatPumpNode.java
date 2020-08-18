package nl.tno.essim.transportsolver.nodes;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.DrivenBySupply;
import esdl.EnergyAsset;
import esdl.EnergyCarrier;
import esdl.GenericProfile;
import esdl.HeatCommodity;
import esdl.HeatPump;
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
public class HeatPumpNode extends ConversionNode {

	protected static final double DEFAULT_HP_COP = 3.5;
	protected double cop;
	protected HeatPump heatPump;
	private String hpName;

	@Builder(builderMethodName = "heatPumpNodeBuilder")
	public HeatPumpNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray,
			JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.heatPump = (HeatPump) asset;
		this.hpName = heatPump.getName() == null ? heatPump.getId() : heatPump.getName();
		this.costInformation = heatPump.getCostInformation();
		this.controlStrategy = heatPump.getControlStrategy();
		this.cop = heatPump.getCOP();
		if (cop < Commons.eps) {
			cop = DEFAULT_HP_COP;
			log.warn("CoP for HP {} is not defined! Defaulting to 3.5", hpName);
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

		if (getRole().equals(Role.PRODUCER)) {
			// PRODUCER
			if (controlStrategy instanceof DrivenByDemand) {
				// PRODUCER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
					// = Make Flexible Producer curve
					double energyOutput = timeStep * power;
					if (costInformation == null) {
						log.warn("Conversion {} is missing cost information!", hpName);
						setCost(0.5);
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
					throw new IllegalStateException("Conversion " + hpName + " cannot be a PRODUCER in this network!");
				}
			} else if (controlStrategy instanceof DrivenBySupply) {
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// = Impossible!
					throw new IllegalStateException("Conversion " + hpName + " cannot be a PRODUCER in this network!");
				} else {
					// PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// = Read from OutPort, make flat production curve
					double energyOutput = Double.NaN;
					Port outPort = null;
					for (Port port : heatPump.getPort()) {
						if (port instanceof OutPort) {
							outPort = port;
						}
					}
					GenericProfile convProfile;
					if (outPort != null) {
						convProfile = Commons.getEnergyProfile(outPort);
					} else {
						// Impossible!
						throw new IllegalStateException("Conversion " + hpName + " should not exist in this network!");
					}

					if (convProfile != null) {
						if (Commons.isPowerProfile(convProfile)) {
							energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
						} else if (Commons.isEnergyProfile(convProfile)) {
							energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
						} else {
							throw new IllegalStateException(
									"Profile in the outPort of " + hpName + " is neither Power nor Energy!");
						}
						makeInflexibleProductionFunction(energyOutput);
					} else {
						throw new IllegalStateException("Profile in the outPort of " + hpName + " is null!");
					}
				}
			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// = Read from OutPort, make flat production curve
				double energyOutput = Double.NaN;
				Port outPort = null;
				for (Port port : heatPump.getPort()) {
					if (port instanceof OutPort) {
						outPort = port;
					}
				}
				GenericProfile convProfile;
				if (outPort != null) {
					convProfile = Commons.getEnergyProfile(outPort);
				} else {
					// Impossible!
					throw new IllegalStateException("Conversion " + hpName + " should not exist in this network!");
				}

				if (convProfile != null) {
					if (Commons.isPowerProfile(convProfile)) {
						energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(convProfile, now));
					} else if (Commons.isEnergyProfile(convProfile)) {
						energyOutput = Commons.aggregateEnergy(Commons.readProfile(convProfile, now));
					} else {
						throw new IllegalStateException(
								"Profile in the outPort of " + hpName + " is neither Power nor Energy!");
					}
					makeInflexibleProductionFunction(energyOutput);
				} else {
					throw new IllegalStateException("Profile in the outPort of " + hpName + " is null!");
				}
			}
		} else {
			// CONSUMER
			if (controlStrategy instanceof DrivenByDemand) {
				// CONSUMER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYDEMAND + DRIVINGCARRIER (Heat Input)
					// = Read from InPort, make flat consumption curve
					GenericProfile profile = null;
					double energyInput = 0.0;
					for (Port port : heatPump.getPort()) {
						if (port instanceof InPort) {
							if (port.getCarrier()
									.equals(carrier)) {
								profile = Commons.getEnergyProfile(port);
								break;
							}
						}
					}
					if (profile != null) {
						if (Commons.isPowerProfile(profile)) {
							energyInput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
						} else if (Commons.isEnergyProfile(profile)) {
							energyInput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
						} else {
							throw new IllegalStateException(
									"Profile in the inPort of " + hpName + " is neither Power nor Energy!");
						}
						makeInflexibleConsumptionFunction(energyInput);
					} else {
						throw new IllegalStateException("Profile in the inPort of " + hpName + " is null!");
					}
				} else {
					// CONSUMER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
					// = Read from InPort, make flat consumption curve
					GenericProfile profile = null;
					double energyInput = 0.0;
					for (Port port : heatPump.getPort()) {
						if (port instanceof InPort) {
							if (port.getCarrier()
									.equals(carrier)) {
								profile = Commons.getEnergyProfile(port);
								break;
							}
						}
					}
					if (profile != null) {
						if (Commons.isPowerProfile(profile)) {
							energyInput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
						} else if (Commons.isEnergyProfile(profile)) {
							energyInput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
						} else {
							throw new IllegalStateException(
									"Profile in the inPort of " + hpName + " is neither Power nor Energy!");
						}
						makeInflexibleConsumptionFunction(energyInput);
					} else {
						throw new IllegalStateException("Profile in the inPort of " + hpName + " is null!");
					}
				}
			} else if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// = Make Flexible Consumer curve
					double inputFactor = 0.0;
					if (carrier instanceof HeatCommodity) {
						inputFactor = (1 - 1 / cop);
					} else {
						inputFactor = 1 / cop;
					}

					double energyInput = timeStep * power * inputFactor;
					if (costInformation == null) {
						log.warn("Conversion {} is missing cost information!", getNodeId());
						setCost(0.5);
					} else {
						GenericProfile marginalCosts = costInformation.getMarginalCosts();
						if (marginalCosts != null) {
							setCost(Commons.aggregateCost(Commons.readProfile(marginalCosts, now)));
						}
					}
					makeAdjustableConsumptionFunction(energyInput);
				} else {
					// CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// = Read from InPort, make flat consumption curve
					GenericProfile profile = null;
					double energyInput = 0.0;
					for (Port port : heatPump.getPort()) {
						if (port.getCarrier()
								.equals(carrier)) {
							profile = Commons.getEnergyProfile(port);
							break;
						}
					}
					if (profile != null) {
						if (Commons.isPowerProfile(profile)) {
							energyInput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
						} else if (Commons.isEnergyProfile(profile)) {
							energyInput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
						} else {
							throw new IllegalStateException(
									"Profile in the inPort of " + hpName + " is neither Power nor Energy!");
						}
						makeInflexibleConsumptionFunction(energyInput);
					} else {
						throw new IllegalStateException("Profile in the inPort of " + hpName + " is null!");
					}
				}
			} else {
				// CONSUMER + DRIVENBYPROFILE
				// = Read from InPort, make flat consumption curve
				DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
				GenericProfile profile = drivenByProfile.getProfile();
				double energyOutput = 0.0;
				double inputFactor = 0.0;
				if (carrier instanceof HeatCommodity) {
					inputFactor = (1 - 1 / cop);
				} else {
					inputFactor = 1 / cop;
				}

				if (profile != null) {
					if (Commons.isPowerProfile(profile)) {
						energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
					} else if (Commons.isEnergyProfile(profile)) {
						energyOutput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
					} else {
						throw new IllegalStateException(
								"Profile in the inPort of " + hpName + " is neither Power nor Energy!");
					}
					double energyInput = energyOutput * inputFactor;
					makeInflexibleConsumptionFunction(energyInput);
				} else {
					throw new IllegalStateException("Profile in the inPort of " + hpName + " is null!");
				}
			}
		}

	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Conversion");

		if (getRole().equals(Role.PRODUCER)) {
			EmissionManager.getInstance(simulationId)
					.addProducer(networkId, heatPump, Math.abs(energy));
			// PRODUCER
			if (controlStrategy instanceof DrivenByDemand) {
				// PRODUCER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYDEMAND + DRIVINGCARRIER
					// = Calculate 2x InputEnergy and write to InPorts
					double electricalInputEnergy = -energy / cop;
					double heatInputEnergy = -energy - electricalInputEnergy;
					Carrier eCarrier = null;
					for (Port port : heatPump.getPort()) {
						if (port instanceof InPort) {
							if (port.getCarrier() instanceof HeatCommodity) {
								Commons.writeProfile(port, timestamp, heatInputEnergy);
							} else {
								eCarrier = port.getCarrier();
								Commons.writeProfile(port, timestamp, electricalInputEnergy);
							}
						}
					}

					if (eCarrier != null) {
						if (eCarrier instanceof EnergyCarrier) {
							EnergyCarrier inputEnergyCarrier = (EnergyCarrier) eCarrier;

							double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
									inputEnergyCarrier.getEmissionUnit());

							double inputCarrierQuantity = Math.abs(electricalInputEnergy);
							double currentInputCarrierCost = Commons
									.aggregateCost(Commons.readProfile(eCarrier.getCost(),
											new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
							double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
							double emission = inputCarrierQuantity * carrierEmission;

							builder.value("emission", emission);
							builder.value("fuelConsumption", inputCarrierQuantity);
							builder.value("cost", inputCarrierCost);
						}
					}
				} else {
					// PRODUCER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
					// = Impossible!!
					throw new IllegalStateException(hpName + " cannot be a producer of " + carrier.getName());
				}
			} else if (controlStrategy instanceof DrivenBySupply) {
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// PRODUCER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// = Impossible!!
					throw new IllegalStateException(hpName + " cannot be a producer of " + carrier.getName());
				} else {
					// PRODUCER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// = Do Nothing!!
				}
			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// = Calculate 2x InputEnergy and write to InPorts
				double electricalInputEnergy = -energy / cop;
				double heatInputEnergy = -energy - electricalInputEnergy;
				Carrier eCarrier = null;
				for (Port port : heatPump.getPort()) {
					if (port instanceof InPort) {
						if (port.getCarrier() instanceof HeatCommodity) {
							Commons.writeProfile(port, timestamp, heatInputEnergy);
						} else {
							eCarrier = port.getCarrier();
							Commons.writeProfile(port, timestamp, electricalInputEnergy);
						}
					}
				}

				if (eCarrier != null) {
					if (eCarrier instanceof EnergyCarrier) {
						EnergyCarrier inputEnergyCarrier = (EnergyCarrier) eCarrier;

						double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
								inputEnergyCarrier.getEmissionUnit());

						double inputCarrierQuantity = Math.abs(electricalInputEnergy);
						double currentInputCarrierCost = Commons.aggregateCost(Commons.readProfile(eCarrier.getCost(),
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
			// CONSUMER
			EmissionManager.getInstance(simulationId)
					.addConsumer(networkId, heatPump, Math.abs(energy));
			if (controlStrategy instanceof DrivenByDemand) {
				// CONSUMER + DRIVENBYDEMAND
				DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
				Carrier drivingCarrier = drivenByDemand.getOutPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYDEMAND + DRIVINGCARRIER
					// = Calculate other InputEnergy and write to other InPort, calculate
					// OutputEnergy and write to OutPort
					double otherInputEnergy = 0.0;
					if (carrier instanceof HeatCommodity) {
						// Other input is Electrical energy
						otherInputEnergy = energy / (cop - 1);
					} else {
						// Other input is Heat energy
						otherInputEnergy = energy * (cop - 1);
						if (carrier instanceof EnergyCarrier) {
							EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

							double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
									inputEnergyCarrier.getEmissionUnit());

							double inputCarrierQuantity = Math.abs(energy);
							double currentInputCarrierCost = Commons
									.aggregateCost(Commons.readProfile(carrier.getCost(),
											new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
							double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
							double emission = inputCarrierQuantity * carrierEmission;

							builder.value("emission", emission);
							builder.value("fuelConsumption", inputCarrierQuantity);
							builder.value("cost", inputCarrierCost);
						}
					}
					double outputEnergy = energy + otherInputEnergy;
					for (Port port : heatPump.getPort()) {
						if (port instanceof OutPort) {
							Commons.writeProfile(port, timestamp, outputEnergy);
						} else {
							if (!port.getCarrier()
									.equals(carrier)) {
								Commons.writeProfile(port, timestamp, otherInputEnergy);
							}
						}
					}
				} else {
					// CONSUMER + DRIVENBYDEMAND + NON-DRIVINGCARRIER
					// = Do Nothing!
				}
			} else if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
				Carrier drivingCarrier = drivenBySupply.getInPort()
						.getCarrier();
				if (drivingCarrier.equals(carrier)) {
					// CONSUMER + DRIVENBYSUPPLY + DRIVINGCARRIER
					// = Calculate other InputEnergy and write to other InPort, calculate
					// OutputEnergy and write
					// to OutPort
					double otherInputEnergy = 0.0;
					if (carrier instanceof HeatCommodity) {
						// Other input is Electrical energy
						otherInputEnergy = energy / (cop - 1);
					} else {
						// Other input is Heat energy
						otherInputEnergy = energy * (cop - 1);
						if (carrier instanceof EnergyCarrier) {
							EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

							double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
									inputEnergyCarrier.getEmissionUnit());

							double inputCarrierQuantity = Math.abs(energy);
							double currentInputCarrierCost = Commons
									.aggregateCost(Commons.readProfile(carrier.getCost(),
											new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
							double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
							double emission = inputCarrierQuantity * carrierEmission;

							builder.value("emission", emission);
							builder.value("fuelConsumption", inputCarrierQuantity);
							builder.value("cost", inputCarrierCost);
						}
					}
					double outputEnergy = energy + otherInputEnergy;
					for (Port port : heatPump.getPort()) {
						if (port instanceof OutPort) {
							Commons.writeProfile(port, timestamp, outputEnergy);
						} else {
							if (!port.getCarrier()
									.equals(carrier)) {
								Commons.writeProfile(port, timestamp, otherInputEnergy);
							}
						}
					}
				} else {
					// CONSUMER + DRIVENBYSUPPLY + NON-DRIVINGCARRIER
					// = Do Nothing
				}
			} else {
				// CONSUMER + DRIVENBYPROFILE
				// = Calculate other InputEnergy and write to other InPort, calculate
				// OutputEnergy and write to
				// OutPort
				double otherInputEnergy = 0.0;
				if (carrier instanceof HeatCommodity) {
					// Other input is Electrical energy
					otherInputEnergy = energy / (cop - 1);
				} else {
					// Other input is Heat energy
					otherInputEnergy = energy * (cop - 1);
					if (carrier instanceof EnergyCarrier) {
						EnergyCarrier inputEnergyCarrier = (EnergyCarrier) carrier;

						double carrierEmission = Commons.toStandardizedUnits(inputEnergyCarrier.getEmission(),
								inputEnergyCarrier.getEmissionUnit());

						double inputCarrierQuantity = Math.abs(energy);
						double currentInputCarrierCost = Commons.aggregateCost(Commons.readProfile(carrier.getCost(),
								new Horizon(timestamp.getTime(), timestamp.getSimulationStepLength())));
						double inputCarrierCost = inputCarrierQuantity * currentInputCarrierCost;
						double emission = inputCarrierQuantity * carrierEmission;

						builder.value("emission", emission);
						builder.value("fuelConsumption", inputCarrierQuantity);
						builder.value("cost", inputCarrierCost);
					}
				}
				double outputEnergy = energy + otherInputEnergy;
				for (Port port : heatPump.getPort()) {
					if (port instanceof OutPort) {
						Commons.writeProfile(port, timestamp, outputEnergy);
					} else {
						if (!port.getCarrier()
								.equals(carrier)) {
							Commons.writeProfile(port, timestamp, otherInputEnergy);
						}
					}
				}
			}
		}
	}

}
