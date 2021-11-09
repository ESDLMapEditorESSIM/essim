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

import org.eclipse.emf.common.util.EList;

import esdl.AbstractBehaviour;
import esdl.Carrier;
import esdl.ControlStrategy;
import esdl.Conversion;
import esdl.CostInformation;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.DrivenBySupply;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.InPort;
import esdl.InputOutputRelation;
import esdl.OutPort;
import esdl.Port;
import esdl.PortRelation;
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
public class ConversionNode extends Node {

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
	private GenericProfile marginalCostProfile;
	private GenericProfile costProfile;
	private HashMap<Port, GenericProfile> costProfileMap;
	private Port mainPort;
	private HashMap<Port, Double> ratioMap;
	private Port dbpPort;
	private GenericProfile dbpProfile;
	private OutPort dbdPort;
	private InPort dbsPort;

	@Builder(builderMethodName = "conversionNodeBuilder")
	public ConversionNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
			int directionFactor, Role role, BidFunction demandFunction, double energy, double cost, Node parent,
			Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
		super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction, energy, cost,
				parent, carrier, children, timeStep, now, connectedPort);
		this.conversion = (Conversion) asset;
		this.conversionName = asset.getName() == null ? asset.getId() : asset.getName();
		this.efficiency = conversion.getEfficiency() == 0.0 ? Commons.DEFAULT_EFFICIENCY : conversion.getEfficiency();
		this.ratioMap = new HashMap<Port, Double>();
		this.power = conversion.getPower();
		this.costInformation = conversion.getCostInformation();
		this.costProfileMap = new HashMap<Port, GenericProfile>();
		if (costInformation != null) {
			marginalCostProfile = costInformation.getMarginalCosts();
		}

		this.controlStrategy = conversion.getControlStrategy();
		if (controlStrategy instanceof DrivenByProfile) {
			DrivenByProfile drivenByProfile = (DrivenByProfile) controlStrategy;
			dbpPort = drivenByProfile.getPort();
			dbpProfile = drivenByProfile.getProfile();
		} else if (controlStrategy instanceof DrivenByDemand) {
			DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
			dbdPort = drivenByDemand.getOutPort();
		} else if (controlStrategy instanceof DrivenBySupply) {
			DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
			dbsPort = drivenBySupply.getInPort();
		}

		for (Port port : conversion.getPort()) {
			if (port instanceof InPort) {
				inputPort = (InPort) port;
				inputCarrier = inputPort.getCarrier();
				costProfile = inputCarrier.getCost();
				costProfileMap.put(port, costProfile);
			} else {
				outputPort = (OutPort) port;
				outputCarrier = outputPort.getCarrier();
			}
		}

		specialConversion = inputCarrier.equals(outputCarrier);

		EList<AbstractBehaviour> conversionBehaviours = conversion.getBehaviour();
		if (conversionBehaviours != null) {
			for (AbstractBehaviour behaviour : conversionBehaviours) {
				if (behaviour instanceof InputOutputRelation) {
					InputOutputRelation conversionTable = (InputOutputRelation) behaviour;
					mainPort = conversionTable.getMainPort();
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
						ratioMap.put(portRelation.getPort(), portRelation.getRatio());
					}
				}
			}
		}
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now) {

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}

		double energyValue = Commons.aggregateEnergy(Commons.readProfile(connectedPort, now));
		if (marginalCostProfile != null) {
			setCost(Commons.aggregateCost(Commons.readProfile(marginalCostProfile, now)));
		} else if (costProfileMap != null && !costProfileMap.isEmpty() && ratioMap != null && !ratioMap.isEmpty()) {
			double costSum = 0.0;
			for (Entry<Port, GenericProfile> portCostPair : costProfileMap.entrySet()) {
				Port inPort = portCostPair.getKey();
				GenericProfile costProfile = portCostPair.getValue();
				Double cost = DEFAULT_MARGINAL_COST;
				double ratio = 1.0;
				if (costProfile != null) {
					cost = Commons.aggregateCost(Commons.readProfile(costProfile, now));
					if (inPort != null) {
						ratio = ratioMap.get(connectedPort) / ratioMap.get(inPort);
					}
				}
				costSum += cost * ratio;
			}
			setCost(costSum);
		} else if (costProfile != null) {
			setCost(Commons.aggregateCost(Commons.readProfile(costProfile, now)));
		} else {
			log.warn("Conversion asset {} is missing cost information! Defaulting to {}", conversionName,
					DEFAULT_MARGINAL_COST);
			setCost(DEFAULT_MARGINAL_COST);
		}

		if (connectedPort instanceof OutPort) {
			// if connected port is an output port (equivalent of 'if role is producer'):
			if (mainPort != null) {
				// if input-output relation table is defined:
				// Either one of the ports were previously solved or this is the first time.
				// Read connected port's profile for this timestep into val.
				if (Double.isNaN(energyValue)) {
					// if val is Double.NaN, this is the first time:
					// Check if DrivenByProfile,
					if (controlStrategy instanceof DrivenByProfile) {
						if (dbpPort == null) {
							throw new IllegalArgumentException("Conversion " + getConversionName()
									+ "'s Driven By Profile control strategy has no port defined!!");
						}
						if (dbpProfile == null) {
							throw new IllegalArgumentException("Conversion " + getConversionName()
									+ "'s Driven By Profile control strategy has no profile defined!!");
						}
						if (Commons.isPowerProfile(dbpProfile)) {
							energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, now));
						} else if (Commons.isEnergyProfile(dbpProfile)) {
							energyValue = Commons.aggregateEnergy(Commons.readProfile(dbpProfile, now));
						} else if (Commons.isPercentageProfile(dbpProfile)) {
							energyValue = timeStep
									* Commons.aggregatePower(Commons.readProfile(dbpProfile, dbpPort, now));
						}
						if (Double.isNaN(energyValue)) {
							energyValue = 0.0;
						}
						if (dbpPort.equals(connectedPort)) {
							// DBP Profile is production profile. So simply read and make inflexible curve.
							makeInflexibleProductionFunction(energyValue);
						} else {
							// DBP Profile is some other profile. So read and make inflexible curve after
							// modifying by port ratio. The calculation is as below:
							// Main Port = P_main
							// P_out = x_out (To be calculated here)
							// P_dbP = x_dbp (Port with DrivenByProfile profile)
							// => P_out = P_main/x_out; P_dbP = P_main/x_dbp
							// => P_out*x_out = P_dbp*x_dbP
							// => P_out = P_dbP*(x_dbp/x_out)
							makeInflexibleProductionFunction(
									energyValue * (ratioMap.get(dbpPort) / ratioMap.get(connectedPort)));
						}
					} else {
						// Make flexible production curve(power/ratio)
						makeAdjustableProductionFunction(power * timeStep / ratioMap.get(connectedPort));
					}
				} else {
					// else:
					// Make inflexible production curve(energy from this port's profile)
					makeInflexibleProductionFunction(energyValue);
				}
			} else {
				// else:
				if (controlStrategy instanceof DrivenByDemand) {
					// if(driven by demand):
					if (dbdPort != null && dbdPort.equals(connectedPort)) {
						// if(driven by demand output port is the same as connected port):
						// Make flexible production curve(power)
						makeAdjustableProductionFunction(power * timeStep);
					} else {
						// else:
						// Multiple output ports are present.
						// Impossible scenario for a non-CHP, non-fuel cell asset with no input-output
						// relation defined.
						throw new IllegalStateException(
								"Non-Cogeneration asset with multiple outputs and no input-output relation defined!");
					}
				} else if (controlStrategy instanceof DrivenBySupply) {
					// else if(driven by supply):
					// One of the input ports was previously solved. That means there is allocation
					// on this port.
					// Read connected port's profile for this time step into val.
					if (!Double.isNaN(energyValue)) {
						// if val is not Double.NaN:
						// Make inflexible production curve(energy from this port's profile)
						makeInflexibleProductionFunction(-energyValue);
					} else {
						// else:
						// Something went wrong in process allocation. Throw an exception until found.
						throw new IllegalStateException("Conversion asset " + conversionName
								+ " has no profile on port " + connectedPort + "! Check previous allocation step!");
					}
				} else if (controlStrategy instanceof DrivenByProfile) {
					// else if(driven by profile):
					// Either one of the ports were previously solved or this is the first time.
					if (Commons.isPowerProfile(dbpProfile)) {
						energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, now));
					} else if (Commons.isEnergyProfile(dbpProfile)) {
						energyValue = Commons.aggregateEnergy(Commons.readProfile(dbpProfile, now));
					} else if (Commons.isPercentageProfile(dbpProfile)) {
						energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, dbpPort, now));
					}
					if (Double.isNaN(energyValue)) {
						energyValue = 0.0;
					}
					if (dbpPort != null && dbpPort.equals(connectedPort)) {
						// if driven-by-profile port is the connected port:
						// This is the first time computation will occur.
						// Read driven-by-profile's profile and make inflexible production curve
						makeInflexibleProductionFunction(energyValue);
					} else {
						// else:
						// This is a production network and DBP profile is a consumption profile.
						// Read driven-by-profile's profile and make inflexible production curve after
						// multiplying with efficiency.
						makeInflexibleProductionFunction(energyValue * efficiency);
					}
				}
			}
		} else {
			// else: (The connected port is an input port (equivalent of 'if role is
			// consumer')):
			if (mainPort != null) {
				// if input-output relation table is defined:
				// Either one of the ports were previously solved or this is the first time.
				// Read connected port's profile for this time step into val.
				if (Double.isNaN(energyValue)) {
					// if val is Double.NaN, this is the first time:
					// Check if DrivenByProfile,
					if (controlStrategy instanceof DrivenByProfile) {
						if (dbpPort == null) {
							throw new IllegalArgumentException("Conversion " + getConversionName()
									+ "'s Driven By Profile control strategy has no port defined!!");
						}
						if (dbpProfile == null) {
							throw new IllegalArgumentException("Conversion " + getConversionName()
									+ "'s Driven By Profile control strategy has no profile defined!!");
						}
						if (Commons.isPowerProfile(dbpProfile)) {
							energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, now));
						} else if (Commons.isEnergyProfile(dbpProfile)) {
							energyValue = Commons.aggregateEnergy(Commons.readProfile(dbpProfile, now));
						} else if (Commons.isPercentageProfile(dbpProfile)) {
							energyValue = timeStep
									* Commons.aggregatePower(Commons.readProfile(dbpProfile, dbpPort, now));
						}
						if (Double.isNaN(energyValue)) {
							energyValue = 0.0;
						}
						if (dbpPort.equals(connectedPort)) {
							// DBP Profile is consumption profile. So simply read and make inflexible curve.
							makeInflexibleConsumptionFunction(energyValue);
						} else {
							// DBP Profile is some other profile. So read and make inflexible curve after
							// modifying by port ratio. The calculation is as below:
							// Main Port = P_main
							// P_in = x_in (To be calculated here)
							// P_dbP = x_dbp (Port with DrivenByProfile profile)
							// => P_in = P_main/x_in; P_dbP = P_main/x_dbp
							// => P_in*x_in = P_dbp*x_dbP
							// => P_in = P_dbP*(x_dbp/x_in)
							makeInflexibleConsumptionFunction(
									energyValue * (ratioMap.get(dbpPort) / ratioMap.get(connectedPort)));
						}
					} else {
						// Make flexible consumption curve(power/ratio)
						makeAdjustableConsumptionFunction(power * timeStep / ratioMap.get(connectedPort));
					}
				} else {
					// else:
					// Make inflexible consumption curve(energy from this port's profile)
					makeInflexibleConsumptionFunction(energyValue);
				}
			} else {
				// else
				if (controlStrategy instanceof DrivenByDemand) {
					// if(driven by demand):
					// There must already be at least one calculation done. So there is allocation
					// on this port.
					// Read connected port's profile for this time step into val.
					if (!Double.isNaN(energyValue)) {
						// if val is not Double.NaN,
						// Make inflexible consumption curve(energy from this port's profile)
						makeInflexibleConsumptionFunction(energyValue);
					} else {
						// else:
						// Something went wrong in process allocation. Throw an exception until found.
						throw new IllegalStateException("Conversion asset " + conversionName
								+ " has no profile on port " + connectedPort + "! Check previous allocation step!");
					}
				} else if (controlStrategy instanceof DrivenBySupply) {
					// else if(driven by supply):
					/// if(driven by demand output port is the same as connected port):
					if (dbsPort != null && dbsPort.equals(connectedPort)) {
						// Make flexible consumption curve(power)
						makeAdjustableConsumptionFunction(power / efficiency);
					} else {
						// else:
						// Make inflexible consumption curve(energy from this port's profile)
						makeInflexibleConsumptionFunction(energyValue);
					}
				} else if (controlStrategy instanceof DrivenByProfile) {
					// else if(driven by profile):
					// Either one of the ports were previously solved or this is the first time.
					if (Commons.isPowerProfile(dbpProfile)) {
						energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, now));
					} else if (Commons.isEnergyProfile(dbpProfile)) {
						energyValue = Commons.aggregateEnergy(Commons.readProfile(dbpProfile, now));
					} else if (Commons.isPercentageProfile(dbpProfile)) {
						energyValue = timeStep * Commons.aggregatePower(Commons.readProfile(dbpProfile, dbpPort, now));
					}
					if (Double.isNaN(energyValue)) {
						energyValue = 0.0;
					}
					if (dbpPort != null && dbpPort.equals(connectedPort)) {
						// if driven-by-profile port is the connected port:
						// This is the first time computation will occur.
						// Read driven-by-profile's profile and make inflexible production curve
						makeInflexibleConsumptionFunction(energyValue);
					} else {
						// This is a consumption network and DBP profile is a production profile.
						// Read driven-by-profile's profile and make inflexible consumption curve after
						// dividing with efficiency.
						makeInflexibleConsumptionFunction(energyValue / efficiency);
					}
				}
			}
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		builder.tag("capability", "Conversion");

		if (mainPort != null) {
			if (controlStrategy instanceof DrivenByDemand) {
				// if(driven by demand):
				if (!dbdPort.equals(connectedPort)) {
					// if(driven by demand output port is NOT the same as connected port):
					// Don't do anything
					return;
				}
			} else if (controlStrategy instanceof DrivenBySupply) {
				// else if(driven by supply):
				if (!dbsPort.equals(connectedPort)) {
					// if(driven by supply input port is NOT the same as connected port):
					// Don't do anything
					return;
				}
			} else if (controlStrategy instanceof DrivenByProfile) {
				// else if(driven by profile):
				// Don't do anything
				return;
			}
			// if input-output relation table is defined:
			double mainPortEnergy = ratioMap.get(connectedPort) * energy;
			log.debug("Energy at {} (connectedPort) = {} J", connectedPort, energy);
			for (Entry<Port, Double> relation : ratioMap.entrySet()) {
				// for each port, calculate energy and write to respective port.
				Port port = relation.getKey();
				if (port.equals(connectedPort)) {
					continue;
				}
				double portEnergy = Math.abs(mainPortEnergy) / relation.getValue();
				log.debug("Energy at {} = {} J", port, portEnergy);
				Commons.writeProfile(port, timestamp, portEnergy);
			}
		} else {
			// else
			if (controlStrategy instanceof DrivenByDemand) {
				// if(driven by demand):
				if (dbdPort.equals(connectedPort)) {
					// if(driven by demand output port is the same as connected port):
					// calculate input = output / efficiency
					// write input to input port
					Commons.writeProfile(inputPort, timestamp, -energy / efficiency);
				}
				// else:
				// we're already at the input port. Nothing more to do.
			} else if (controlStrategy instanceof DrivenBySupply) {
				// else if(driven by supply):
				if (dbsPort.equals(connectedPort)) {
					// if(driven by supply input port is the same as connected port):
					// calculate output = input * efficiency
					// write output to output port
					Commons.writeProfile(outputPort, timestamp, -energy * efficiency);
				}
				// else:
				// we're already at the output port. Nothing more to do.
			} else if (controlStrategy instanceof DrivenByProfile) {
				// else if(driven by profile):
				if (dbpPort.equals(connectedPort)) {
					// if driven-by-profile port is the connected port:
					if (connectedPort instanceof InPort) {
						// if connected port is an input port:
						// calculate output = input * efficiency
						// write output to output port
						Commons.writeProfile(outputPort, timestamp, -energy * efficiency);
					} else {
						// else:
						// calculate input = output / efficiency
						// write input to input port
						Commons.writeProfile(outputPort, timestamp, -energy / efficiency);
					}
				}
				// else:
				// do nothing
			}
		}
	}
}
