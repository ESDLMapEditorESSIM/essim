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
import esdl.ControlStrategy;
import esdl.CostInformation;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.DrivenBySupply;
import esdl.ElectricityCommodity;
import esdl.EnergyAsset;
import esdl.GasCommodity;
import esdl.GenericProfile;
import esdl.HeatCommodity;
import esdl.HybridHeatpump;
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
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class HybridHeatpumpNode extends Node {

	protected static final double DEFAULT_HP_COP = 3.5;
	private static final double DEFAULT_EFFICIENCY = 0.6;
	protected double cop;
	protected double efficiency;
	protected HybridHeatpump hybridHeatPump;
	private String hhpName;
	private GenericProfile elecCostProfile = null;
	private GenericProfile gasCostProfile = null;
	private GenericProfile marginalCostProfile;
	private InPort eInPort;
	private InPort gInPort;
	private OutPort hOutPort;
	private double maxElecPowerThermal;
	private double maxGasPowerThermal;
	private CostInformation costInformation;
	private ControlStrategy controlStrategy;

	@Builder(builderMethodName = "hybridHeatpumpNodeBuilder")
	public HybridHeatpumpNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
			Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
				energy, cost, parent, carrier, children, timeStep, now, connectedPort);
		this.timeStep = timeStep;
		this.hybridHeatPump = (HybridHeatpump) asset;
		this.hhpName = hybridHeatPump.getName() == null ? hybridHeatPump.getId() : hybridHeatPump.getName();
		costInformation = hybridHeatPump.getCostInformation();
		if (costInformation != null) {
			marginalCostProfile = costInformation.getMarginalCosts();
		}
		for (Port port : hybridHeatPump.getPort()) {
			if (port instanceof InPort) {
				if (port.getCarrier() instanceof ElectricityCommodity) {
					ElectricityCommodity commodity = (ElectricityCommodity) port.getCarrier();
					elecCostProfile = commodity.getCost();
					eInPort = (InPort) port;
				} else if (port.getCarrier() instanceof GasCommodity) {
					GasCommodity commodity = (GasCommodity) port.getCarrier();
					gasCostProfile = commodity.getCost();
					gInPort = (InPort) port;
				}
			} else {
				if (port instanceof OutPort) {
					if (port.getCarrier() instanceof HeatCommodity) {
						hOutPort = (OutPort) port;
					}
				}
			}
		}
		if (eInPort == null || gInPort == null || hOutPort == null) {
			throw new IllegalArgumentException(
					"Hybrid heat pump supports only Heat, Electricity and Gas Commodities on its output and input ports respectively. Please check the commodities on the ports of HHP "
							+ hhpName);
		}

		controlStrategy = hybridHeatPump.getControlStrategy();

		maxElecPowerThermal = hybridHeatPump.getHeatpumpThermalPower();
		if (maxElecPowerThermal < Commons.eps) {
			throw new IllegalArgumentException(
					"HeatpumpThermalPower attribute of hybrid heat pump " + hhpName + " not specified!");
		}
		cop = hybridHeatPump.getHeatpumpCOP();
		if (cop < Commons.eps) {
			cop = DEFAULT_HP_COP;
			log.warn("CoP for HHP {} is not defined! Defaulting to {}", hhpName, DEFAULT_HP_COP);
		}

		maxGasPowerThermal = hybridHeatPump.getGasHeaterThermalPower();
		if (maxGasPowerThermal < Commons.eps) {
			throw new IllegalArgumentException(
					"GasHeaterThermalPower attribute of hybrid heat pump " + hhpName + " not specified!");
		}
		efficiency = hybridHeatPump.getGasHeaterEfficiency();
		if (efficiency < Commons.eps) {
			efficiency = DEFAULT_EFFICIENCY;
			log.warn("Efficiency for HHP {} is not defined! Defaulting to {}", hhpName, DEFAULT_EFFICIENCY);
		}
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		// @formatter:off
		// This HHP is modelled like so:
		// =============================
		// if Heat Output < Max Electrical Thermal Output,
		// Heat Output = COP * Electrical Input
		// if Max Electrical Thermal Output <= Heat Output <= Max Electrical Thermal
		// Output + Max Gas Thermal Output,
		// Heat Output = Max Electrical Thermal Output + Eff * Gas Input
		// @formatter:on

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
				// Make a flexible production curve based on Eth + Gth
				double totalPossibleThermalOutput = maxElecPowerThermal + maxGasPowerThermal;
				double energyOutput = timeStep * totalPossibleThermalOutput;
				if (marginalCostProfile != null) {
					setCost(Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now)));
				} else {
					log.warn("Hybrid heat pump {} is missing cost information! Defaulting to {}", hhpName,
							DEFAULT_MARGINAL_COST);
					setCost(DEFAULT_MARGINAL_COST);
				}

				makeAdjustableProductionFunction(energyOutput);

			} else if (controlStrategy instanceof DrivenBySupply) {
				// PRODUCER + DRIVENBYDEMAND
				// At least one of the input ports was already solved, so read connected port
				// and make an inflexible production curve
				GenericProfile energyOutputProfile = Commons.getEnergyProfile(connectedPort);
				double energyOutput = Commons.aggregateEnergy(Commons.readProfile(energyOutputProfile, now));
				makeInflexibleProductionFunction(energyOutput);

			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// Read profile, check carrier on port, multiply with the corresponding
				// efficiency/cop and make inflexible production curve
				DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
				GenericProfile drivingProfile = drivenByProfile.getProfile();
				if (drivenByProfile.getPort() == null) {
					throw new IllegalArgumentException(
							"DrivenByProfile control strategy for HHP " + hhpName + " has no port attached to it!");
				} else {
					double profileValue = 0.0;
					double energyOutput = 0.0;
					if (drivingProfile != null) {
						if (Commons.isPowerProfile(drivingProfile)) {
							profileValue = timeStep * Commons.aggregatePower(Commons.readProfile(drivingProfile, now));
						} else if (Commons.isEnergyProfile(drivingProfile)) {
							profileValue = Commons.aggregateEnergy(Commons.readProfile(drivingProfile, now));
						} else {
							throw new IllegalStateException("Profile in the DrivenByProfile control strategy of HHP "
									+ hhpName + " is neither Power nor Energy!");
						}
					} else {
						throw new IllegalStateException(
								"There is no profile in the DrivenByProfile control strategy of HHP " + hhpName + "!");
					}

					if (drivenByProfile.getPort().equals(hOutPort)) {
						// If heat profile is given, the value in the profile is the heat output which
						// cannot exceed the
						// rated electrical and gas thermal outputs of the device
						energyOutput = Math.min(profileValue, (maxElecPowerThermal + maxGasPowerThermal) * timeStep);
					} else {
						throw new IllegalArgumentException(
								"HHP can only be driven by a heat output profile. DrivenByProfile control strategy for HHP "
										+ hhpName + " is not attached to a heat port in this asset!");
					}
					makeInflexibleProductionFunction(energyOutput);
				}
			}
		} else {
			// CONSUMER
			if (controlStrategy instanceof DrivenByDemand) {
				// CONSUMER + DRIVENBYDEMAND
				// Heat network was already solved at this point, so read connected port and
				// make an inflexible consumption curve
				GenericProfile energyInputProfile = Commons.getEnergyProfile(connectedPort);
				double energyInput = Commons.aggregateEnergy(Commons.readProfile(energyInputProfile, now));
				makeInflexibleConsumptionFunction(energyInput);

			} else if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				// Make a flexible consumption curve based on carrier
				double energyInput;
				Double carrierCost = null;
				if (connectedPort.equals(eInPort)) {
					energyInput = maxElecPowerThermal / cop;
					if (elecCostProfile != null) {
						carrierCost = Commons.aggregateCost(Commons.readProfile(elecCostProfile, now));
					}
				} else if (connectedPort.equals(gInPort)) {
					energyInput = maxGasPowerThermal / efficiency;
					if (gasCostProfile != null) {
						carrierCost = Commons.aggregateCost(Commons.readProfile(gasCostProfile, now));
					}
				} else {
					throw new IllegalStateException("Hybrid heat pump " + hhpName
							+ " cannot be a consumer in this network -> " + networkId + "!");
				}
				if (marginalCostProfile != null) {
					setCost(Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now)));
				} else if (carrierCost != null) {
					setCost(carrierCost);
				} else {
					log.warn("Hybrid heat pump {} is missing cost information! Defaulting to {}", hhpName,
							DEFAULT_MARGINAL_COST);
					setCost(DEFAULT_MARGINAL_COST);
				}
				makeAdjustableConsumptionFunction(energyInput);

			} else if (controlStrategy instanceof DrivenByProfile) {
				// CONSUMER + DRIVENBYPROFILE
				// Read profile, check carrier on port, divide by the corresponding
				// efficiency/cop and make inflexible consumption curve
				DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
				GenericProfile drivingProfile = drivenByProfile.getProfile();
				double profileValue = 0.0;
				if (drivingProfile != null) {
					if (Commons.isPowerProfile(drivingProfile)) {
						profileValue = timeStep * Commons.aggregatePower(Commons.readProfile(drivingProfile, now));
					} else if (Commons.isEnergyProfile(drivingProfile)) {
						profileValue = Commons.aggregateEnergy(Commons.readProfile(drivingProfile, now));
					} else {
						throw new IllegalStateException("Profile in the DrivenByProfile control strategy of HHP "
								+ hhpName + " is neither Power nor Energy!");
					}
				} else {
					throw new IllegalStateException(
							"There is no profile in the DrivenByProfile control strategy of HHP " + hhpName + "!");
				}

				if (drivenByProfile.getPort().equals(hOutPort)) {
					double eInputEnergy = 0.0;
					double gInputEnergy = 0.0;
					if (profileValue < maxElecPowerThermal * timeStep) {
						eInputEnergy = profileValue / cop;
					} else if ((profileValue >= maxElecPowerThermal * timeStep)
							&& (profileValue <= (maxElecPowerThermal + maxGasPowerThermal) * timeStep)) {
						eInputEnergy = (maxElecPowerThermal * timeStep) / cop;
						gInputEnergy = (profileValue - (maxElecPowerThermal * timeStep)) / efficiency;
					} else {
						eInputEnergy = (maxElecPowerThermal * timeStep) / cop;
						gInputEnergy = (maxGasPowerThermal * timeStep) / efficiency;
					}

					if (connectedPort.equals(eInPort)) {
						makeInflexibleConsumptionFunction(eInputEnergy);
					} else if (connectedPort.equals(gInPort)) {
						makeInflexibleConsumptionFunction(gInputEnergy);
					} else {
						throw new IllegalStateException("Hybrid heat pump " + hhpName
								+ " cannot be a consumer in this network -> " + networkId + "!");
					}
				} else {
					throw new IllegalArgumentException(
							"HHP can only be driven by a heat output profile. DrivenByProfile control strategy for HHP "
									+ hhpName + " is not attached to a heat port in this asset!");
				}
			}
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Conversion");
		timeStep = timestamp.getSimulationStepLength().getSeconds();

		if (getRole().equals(Role.PRODUCER)) {
			// PRODUCER
			if (controlStrategy instanceof DrivenByDemand) {
				// PRODUCER + DRIVENBYDEMAND
				// This is a heat allocation from a flexible bid curve -> Calculate necessary
				// electricity and gas and write to respective ports
				double eInputEnergy = 0.0;
				double gInputEnergy = 0.0;
				double hOutputEnergy = Math.abs(energy);
				if (hOutputEnergy < maxElecPowerThermal * timeStep) {
					eInputEnergy = hOutputEnergy / cop;
				} else if ((hOutputEnergy >= maxElecPowerThermal * timeStep)
						&& (hOutputEnergy <= (maxElecPowerThermal + maxGasPowerThermal) * timeStep)) {
					eInputEnergy = (maxElecPowerThermal * timeStep) / cop;
					gInputEnergy = (hOutputEnergy - (maxElecPowerThermal * timeStep)) / efficiency;
				} else {
					eInputEnergy = (maxElecPowerThermal * timeStep) / cop;
					gInputEnergy = (maxGasPowerThermal * timeStep) / efficiency;
				}
				Commons.writeProfile(eInPort, timestamp, eInputEnergy);
				Commons.writeProfile(gInPort, timestamp, gInputEnergy);

			} else if (controlStrategy instanceof DrivenBySupply) {
				// PRODUCER + DRIVENBYSUPPLY
				// This is a heat allocation from an inflexible bid curve -> Don't have to do
				// anything as this is the last round of calculation for the HHP in this time
				// step.
			} else if (controlStrategy instanceof DrivenByProfile) {
				// PRODUCER + DRIVENBYPROFILE
				// This is a heat allocation from an inflexible bid curve -> Don't do anything
				// The other ports' bid curves will be constructed independently from the
				// attached profile.
			}
		} else {
			// CONSUMER
			if (controlStrategy instanceof DrivenByDemand) {
				// CONSUMER + DRIVENBYDEMAND
				// This is an elec/gas allocation from an inflexible bid curve -> Don't have to
				// do anything as this is the last round of calculation for the HHP in this time
				// step.
			} else if (controlStrategy instanceof DrivenBySupply) {
				// CONSUMER + DRIVENBYSUPPLY
				// This is an elec/gas allocation from a flexible bid curve -> Calculate
				// necessary other input and heat generated and write to respective ports
				double gInputEnergy = 0.0;
				double eInputEnergy = 0.0;
				double hOutputEnergy = 0.0;
				if (connectedPort.equals(eInPort)) {
					// Allocation is electricity, then gas input is zero and only heat output need
					// be calculated
					eInputEnergy = energy;
					hOutputEnergy = energy * cop;
				} else if (connectedPort.equals(gInPort)) {
					// Allocation is gas, then electricity input is max and heat output need be
					// calculated
					eInputEnergy = (maxElecPowerThermal / cop) * timeStep;
					gInputEnergy = energy;
					hOutputEnergy = (gInputEnergy * efficiency) + eInputEnergy;
				} else {
					throw new IllegalStateException("Hybrid heat pump " + hhpName
							+ " cannot be a consumer in this network -> " + networkId + "!");
				}
				Commons.writeProfile(eInPort, timestamp, eInputEnergy);
				Commons.writeProfile(gInPort, timestamp, gInputEnergy);
				Commons.writeProfile(hOutPort, timestamp, hOutputEnergy);

			} else {
				// CONSUMER + DRIVENBYPROFILE
				// This is an elec/gas allocation from an inflexible bid curve -> Don't do
				// anything. The other ports' bid curves will be constructed independently from
				// the attached profile.

			}
		}
	}
}
