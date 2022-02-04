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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import esdl.AbstractBuilding;
import esdl.Area;
import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.Sector;
import esdl.Transport;
import esdl.impl.ItemImpl;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@Builder
@Data
@Slf4j
public abstract class Node implements INode {
	protected static final double DEFAULT_MARGINAL_COST = 0.5;
	private static final double price_delta = 0.01;
	private static final double pmin = 0.0;
	private static final double pmax = 1.0;

	protected String simulationId;
	protected String nodeId;
	protected String address;
	protected String networkId;
	protected EnergyAsset asset;
	protected String esdlString;
	protected int directionFactor;
	protected Role role;
	protected BidFunction demandFunction;
	protected double energy;
	protected double cost;
	protected Node parent;
	protected Carrier carrier;
	protected List<Node> children;
	protected long timeStep;
	protected Horizon now;

	public static class NodeBuilder {
		private static final String NODE = "Node";
		private static final String NODE_PACKAGE_NAME = Node.class.getPackage().getName();

		private NodeConfiguration config;

		public NodeBuilder asset(EnergyAsset asset) {
			this.asset = asset;
			this.address = createAddress();
			return this;
		}

		public NodeBuilder config(NodeConfiguration config) {
			this.config = config;
			return this;
		}

		public Node build() {
			Node node = null;
			if (this.config != null) {
				node = new RemoteLogicNode(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor,
						role, demandFunction, energy, cost, parent, carrier, children, timeStep, now, config);
			} else if (asset != null) {
				Class<?> assetNodeClass = null;
				classSearch: for (Class<?> clazz = asset.getClass(); !clazz.equals(ItemImpl.class); clazz = clazz
						.getSuperclass()) {
					for (Class<?> interfaze : clazz.getInterfaces()) {
						String nodeName = NODE_PACKAGE_NAME + "." + interfaze.getSimpleName() + NODE;
						try {
							assetNodeClass = Class.forName(nodeName);
						} catch (ClassNotFoundException e) {
							continue;
						}
						break classSearch;
					}
				}

				log.debug("Asset " + asset.getClass().getInterfaces()[0].getSimpleName() + " is implemented as : "
						+ assetNodeClass.getSimpleName());

				if (assetNodeClass != null) {
					try {
						node = (Node) assetNodeClass.getConstructor(String.class, String.class, String.class,
								String.class, EnergyAsset.class, String.class, int.class, Role.class, BidFunction.class,
								double.class, double.class, Node.class, Carrier.class, List.class, long.class,
								Horizon.class).newInstance(simulationId, nodeId, address, networkId, asset, esdlString,
										directionFactor, role, demandFunction, energy, cost, parent, carrier, children,
										timeStep, now);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			return node;
		}

		private String createAddress() {
			String address = null;
			if (asset != null) {
				if (asset.getArea() != null) {
					address = getArea(asset.getArea(), address);
				} else {
					address = getArea(asset.getContainingBuilding(), address);
				}
			}
			return address;
		}

		private String getArea(AbstractBuilding building, String areaString) {
			if (areaString == null) {
				areaString = building.getName() == null ? building.getId() : building.getName();
			} else {
				areaString = areaString + ", " + (building.getName() == null ? building.getId() : building.getName());
			}
			if (building.getContainingBuilding() != null) {
				areaString = getArea(building.getContainingBuilding(), areaString);
			} else {
				areaString = getArea(building.getArea(), areaString);
			}
			return areaString;
		}

		private String getArea(Area area, String areaString) {
			if (areaString == null) {
				areaString = area.getName() == null ? area.getId() : area.getName();
			} else {
				areaString = areaString + ", " + (area.getName() == null ? area.getId() : area.getName());
			}
			if (area.getContainingArea() != null) {
				areaString = getArea(area.getContainingArea(), areaString);
			}
			return areaString;
		}

	}

	public void addChild(Node n) {
		if (children == null) {
			children = new ArrayList<Node>();
		}
		n.setParent(this);
		children.add(n);
	}

	public Node findNodeById(String id) {
		if (nodeId.equals(id)) {
			return this;
		}
		if (children != null) {
			for (Node child : children) {
				child.findNodeById(id);
			}
		}

		return null;
	}

	public void findNodesByAssetType(Class<?> clazz, List<Node> nodeList) {
		if (clazz.isInstance(asset)) {
			nodeList.add(this);
		}
		if (children != null) {
			for (Node child : children) {
				child.findNodesByAssetType(clazz, nodeList);
			}
		}
	}

	public void findDeviceNodes(List<Node> nodeList) {
		if (!(asset instanceof Transport)) {
			nodeList.add(this);
		}
		if (children != null) {
			for (Node child : children) {
				child.findDeviceNodes(nodeList);
			}
		}
	}

	public BidFunction aggregateDemandFunction() {
		if (children != null) {
			BidFunction aggregatedFunction = new BidFunction();
			for (Node child : children) {
				BidFunction nodeFunction = child.aggregateDemandFunction();
				aggregatedFunction = sumCurves(nodeFunction, aggregatedFunction);
			}
			if (this.asset instanceof Transport) {
				setDemandFunction(aggregatedFunction);
			} else {
				aggregatedFunction = sumCurves(getDemandFunction(), aggregatedFunction);
			}
			return aggregatedFunction;
		} else {
			BidFunction demandFunction = getDemandFunction();
			if (demandFunction == null) {
				demandFunction = new BidFunction();
				demandFunction.addPoint(pmin, 0.0);
				demandFunction.addPoint(pmax, 0.0);
				setDemandFunction(demandFunction);
			}
			return getDemandFunction();
		}
	}

	public void normaliseCosts() {
		double[] minMaxCosts = findMinMaxCosts(Double.MAX_VALUE, Double.MIN_VALUE);
		normaliseCosts(minMaxCosts);
	}

	private double[] findMinMaxCosts(double min, double max) {
		if (children != null) {
			for (Node child : children) {
				double[] res = child.findMinMaxCosts(min, max);
				if (res[0] < min)
					min = res[0];
				if (res[1] > max)
					max = res[1];
			}
		}
		if (cost < min)
			min = cost;
		if (cost > max)
			max = cost;

		return new double[] { min, max };
	}

	private void normaliseCosts(double[] minMaxCosts) {
		if (children != null) {
			for (Node child : children) {
				child.normaliseCosts(minMaxCosts);
			}
		}
		cost = (cost - minMaxCosts[0]) / (minMaxCosts[1] - minMaxCosts[0]);
	}

	public double[] allocateAndPropagate(BidFunction summedFunction, List<Observation> observations,
			EssimTime timestamp) {
		// Allocate
		double balancingPrice = findPriceFromCurve(summedFunction, 0.0);
		double imbalance = findDemandFromCurve(summedFunction, balancingPrice);

		// Propagate
		propagate(balancingPrice, observations, timestamp);

		double[] vals = { imbalance, balancingPrice };
		return vals;
	}

	public JSONObject getJSONString() {
		JSONObject nodeObj = new JSONObject().put("name", getNodeId() + "(" + getRole() + ")");
		if (parent != null) {
			nodeObj.put("parent", parent.getNodeId() + "(" + parent.getRole() + ")");
		} else {
			nodeObj.put("parent", "null");
		}

		JSONArray childrenJSON = null;
		if (children != null && !children.isEmpty()) {
			childrenJSON = new JSONArray();
			for (Node node : children) {
				JSONObject childJSON = node.getJSONString();
				childrenJSON.put(childJSON);
			}
		}
		if (childrenJSON != null) {
			nodeObj.put("children", childrenJSON);
		}

		return nodeObj;
	}

	public void makeInflexibleProductionFunction(double emax) {
		energy = -emax;

		demandFunction = new BidFunction();
		demandFunction.setMarginalCost(pmin);
		demandFunction.addPoint(pmin, -emax);
		demandFunction.addPoint(pmax, -emax);
	}

	public void makeInflexibleConsumptionFunction(double emax) {
		energy = emax;

		demandFunction = new BidFunction();
		demandFunction.setMarginalCost(pmin);
		demandFunction.addPoint(pmin, emax);
		demandFunction.addPoint(pmax, emax);
	}

	public void makeAdjustableProductionFunction(double emax) {
		energy = -emax;

		demandFunction = new BidFunction();
		demandFunction.setMarginalCost(cost);
		demandFunction.addPoint(pmin, 0.0);
		demandFunction.addPoint(cost, 0.0);
		demandFunction.addPoint(cost + price_delta, energy);
		demandFunction.addPoint(pmax, energy);
	}

	public void makeAdjustableConsumptionFunction(double emax) {
		energy = emax;
		demandFunction = new BidFunction();
		demandFunction.setMarginalCost(cost);
		demandFunction.addPoint(pmin, energy);
		if (cost != 0.0) {
			demandFunction.addPoint(cost, 0.0);
		}
		demandFunction.addPoint(pmax, 0.0);
	}

	public void makeStorageFunction(double ecmax, double edmax, double soc, Double mc1, Double mc2) {

		// Marginal cost of storage moves inversely as SOC.
		// Higher the SOC, the more eager it is to start producing.
		// cost = 0.9 - 0.8 * soc;
		// cost = 1 - 0.9 * soc * soc;
		// cost = Math.max(0, Math.min(1, 1 - soc));
		double delta = 0.01;
		cost = 0.15;
		if (mc1 == null) {
			mc1 = Math.max(0.0, 0.95 * cost);
		}
		if (mc2 == null) {
			mc2 = Math.min(1.0, 1.05 * cost);
		}

		demandFunction = new BidFunction();
		demandFunction.setMarginalCost(mc1);
		demandFunction.addPoint(pmin, ecmax);
		demandFunction.addPoint(Math.max(pmin, Math.min(mc1, pmax)), ecmax);
		demandFunction.addPoint(Math.max(pmin, Math.min(mc1 + delta, pmax)), 0.0);
		demandFunction.addPoint(Math.max(pmin, Math.min(mc2, pmax)), 0.0);
		demandFunction.addPoint(Math.max(pmin, Math.min(mc2 + delta, pmax)), -edmax);
		demandFunction.addPoint(pmax, -edmax);
	}

	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();

		bldr.append(nodeId);
		bldr.append("(");
		bldr.append(asset.getClass().getInterfaces()[0].getSimpleName());
		bldr.append(") : ");
		bldr.append(role.toString());

		return bldr.toString();
	}

	private BidFunction sumCurves(BidFunction a, BidFunction b) {
		return BidFunction.sumCurves(a, b);
	}

	private void propagate(double price, List<Observation> observations, EssimTime timestamp) {
		// Allocate
		energy = findDemandFromCurve(demandFunction, price);

		// Make observation
		ObservationBuilder builder = Observation.builder().observedAt(timestamp.getTime()).tag("assetId", asset.getId())
				.tag("assetName", asset.getName() == null ? "UnnamedAsset" : asset.getName())
				.tag("assetClass", asset.getClass().getInterfaces()[0].getSimpleName()).tag("address", address)
				.tag("carrierId", carrier.getId())
				.tag("carrierName", carrier.getName() == null ? "UnnamedCarrier" : carrier.getName())
				.value("allocationEnergy", energy)
				.value("allocationPower", energy / timestamp.getSimulationStepLength().getSeconds())
				.value("marginalCost", cost);

		// Tag sector if defined
		String sectorName = "DefaultSector";
		Sector sector = asset.getSector();
		if (sector != null) {
			if (sector.getName() != null) {
				sectorName = sector.getName();
			}
		}
		builder.tag("sector", sectorName);

		// Process Allocation for this node
		processAllocation(timestamp, builder, price);

		// Build observations
		observations.add(builder.build());

		// Propagate
		if (children != null) {
			for (Node child : children) {
				child.propagate(price, observations, timestamp);
			}
		}

	}

	private double findDemandFromCurve(BidFunction demandFunction, double price) {
		return demandFunction.findDemandFromCurve(price);
	}

	private double findPriceFromCurve(BidFunction demandFunction, double demand) {
		return demandFunction.findPriceFromCurve(demand);
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		if (this.timeStep == 0l) {
			this.timeStep = timeStep;
		}
		if (this.now == null) {
			this.now = now;
		}
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {

	}

	// Consider renaming for consumers
	// Checks the commissioning and decommissioning dates of the asset
	public boolean isOperational(Horizon now) {
		// Only assets whose commissioning date is at or before the start time of the
		// simulation step construct a bid curve
		// AND
		// Only assets whose decommissioning date is after the start time of the
		// simulation step construct a bid curve

		boolean isOperational = false;

		if (asset.getCommissioningDate() == null && asset.getDecommissioningDate() == null) {
			isOperational = true;
		} else {
			if (asset.getCommissioningDate() != null && asset.getDecommissioningDate() == null) {
				LocalDateTime converted_cd = EssimTime.dateToLocalDateTime(asset.getCommissioningDate());
				if (converted_cd.isBefore(now.getStartTime()) || converted_cd.isEqual(now.getStartTime()))
					isOperational = true;
			} else if (asset.getCommissioningDate() == null && asset.getDecommissioningDate() != null) {
				LocalDateTime converted_dd = EssimTime.dateToLocalDateTime(asset.getDecommissioningDate());
				if (converted_dd.isAfter(now.getStartTime()))
					isOperational = true;
			} else {
				LocalDateTime converted_cd = EssimTime.dateToLocalDateTime(asset.getCommissioningDate());
				LocalDateTime converted_dd = EssimTime.dateToLocalDateTime(asset.getDecommissioningDate());

				if ((converted_cd.isBefore(now.getStartTime()) || converted_cd.isEqual(now.getStartTime()))
						&& converted_dd.isAfter(now.getStartTime()))
					isOperational = true;
			}
		}

		return isOperational;

	}

}
