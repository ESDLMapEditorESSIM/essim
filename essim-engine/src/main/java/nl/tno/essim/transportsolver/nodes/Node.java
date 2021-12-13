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
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

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
	private static final double eps = 1e-12;
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
	protected TreeMap<Double, Double> demandFunction;
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
				node = new RemoteLogicNode(simulationId, nodeId, address, networkId, asset, esdlString,
						directionFactor, role, demandFunction, energy, cost, parent, carrier, children, timeStep, now,
						config);
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
								String.class, EnergyAsset.class, String.class, int.class,
								Role.class, TreeMap.class, double.class, double.class, Node.class, Carrier.class,
								List.class, long.class, Horizon.class).newInstance(simulationId, nodeId, address,
										networkId, asset, esdlString, directionFactor, role,
										demandFunction, energy, cost, parent, carrier, children, timeStep, now);
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

	public TreeMap<Double, Double> aggregateDemandFunction() {
		if (children != null) {
			TreeMap<Double, Double> aggregatedFunction = new TreeMap<Double, Double>();
			for (Node child : children) {
				TreeMap<Double, Double> nodeFunction = child.aggregateDemandFunction();
				aggregatedFunction = sumCurves(nodeFunction, aggregatedFunction);
			}
			if (this.asset instanceof Transport) {
				setDemandFunction(aggregatedFunction);
			} else {
				aggregatedFunction = sumCurves(getDemandFunction(), aggregatedFunction);
			}
			return aggregatedFunction;
		} else {
			TreeMap<Double, Double> demandFunction = getDemandFunction();
			if (demandFunction == null) {
				demandFunction = new TreeMap<Double, Double>();
				demandFunction.put(pmin, 0.0);
				demandFunction.put(pmax, 0.0);
				setDemandFunction(demandFunction);
			}
			return getDemandFunction();
		}
	}

	public void normaliseCosts() {
		double[] minMaxCosts = findMinMaxCosts(Double.MAX_VALUE, Double.MIN_VALUE);
		normaliseCosts(minMaxCosts);
	}

	public double[] allocateAndPropagate(TreeMap<Double, Double> summedFunction, List<Observation> observations,
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

		demandFunction = new TreeMap<Double, Double>();
		demandFunction.put(pmin, -emax);
		demandFunction.put(pmax, -emax);
	}

	public void makeInflexibleConsumptionFunction(double emax) {
		energy = emax;

		demandFunction = new TreeMap<Double, Double>();
		demandFunction.put(pmin, emax);
		demandFunction.put(pmax, emax);
	}

	public void makeAdjustableProductionFunction(double emax) {
		energy = -emax;

		demandFunction = new TreeMap<Double, Double>();

		if (cost == 0.0) {
			cost = pmax / 2;
		}

		demandFunction.put(pmin, 0.0);
		demandFunction.put(cost, 0.0);
		demandFunction.put(cost + price_delta, energy);
		demandFunction.put(pmax, energy);
	}

	public void makeAdjustableConsumptionFunction(double emax) {
		energy = emax;
		demandFunction = new TreeMap<Double, Double>();
		demandFunction.put(pmin, energy);
		if (cost != 0.0) {
			demandFunction.put(cost, 0.0);
		}
		demandFunction.put(pmax, 0.0);
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
		
		demandFunction = new TreeMap<Double, Double>();

		demandFunction.put(pmin, ecmax);
		demandFunction.put(Math.max(pmin, Math.min(mc1, pmax)), ecmax);
		demandFunction.put(Math.max(pmin, Math.min(mc1+delta, pmax)), 0.0);
		demandFunction.put(Math.max(pmin, Math.min(mc2, pmax)), 0.0);
		demandFunction.put(Math.max(pmin, Math.min(mc2+delta, pmax)), -edmax);
		demandFunction.put(pmax, -edmax);
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

	private TreeMap<Double, Double> sumCurves(TreeMap<Double, Double> a, TreeMap<Double, Double> b) {
		TreeMap<Double, Double> sum = new TreeMap<Double, Double>();
		TreeSet<Double> allPrices = new TreeSet<Double>();
		allPrices.addAll(a.keySet());
		allPrices.addAll(b.keySet());
		for (double price : allPrices) {
			double aValue = findDemandFromCurve(a, price);
			double bValue = findDemandFromCurve(b, price);
			sum.put(price, aValue + bValue);
		}
		return sum;
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
				.value("allocationPower", energy / timestamp.getSimulationStepLength().getSeconds());

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

	private double findDemandFromCurve(TreeMap<Double, Double> demandFunction, double price) {
		if (demandFunction.isEmpty()) {
			return 0.0;
		}

		if (demandFunction.containsKey(price)) {
			return demandFunction.get(price);
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : demandFunction.keySet()) {
			if (p > price) {
				p2 = p;
				v2 = demandFunction.get(p2);
				break;
			}
			p1 = p;
			v1 = demandFunction.get(p1);
		}

		if (Double.isNaN(p1)) {
			return v2;
		} else if (Double.isNaN(p2)) {
			return v1;
		} else {
			return v1 + (((v2 - v1) / (p2 - p1)) * (price - p1));
		}
	}

	private double findPriceFromCurve(TreeMap<Double, Double> demandFunction, double demand) {
		for (Entry<Double, Double> entry : demandFunction.entrySet()) {
			if (Math.abs(entry.getValue() - demand) < eps) {
				return entry.getKey();
			}
		}

		double p1 = Double.NaN;
		double p2 = Double.NaN;
		double v1 = Double.NaN;
		double v2 = Double.NaN;
		for (double p : demandFunction.keySet()) {
			double v = demandFunction.get(p);
			if (v < demand) {
				p2 = p;
				v2 = v;
				break;
			}
			p1 = p;
			v1 = v;
		}

		if (Double.isNaN(p1)) {
			return p2;
		} else if (Double.isNaN(p2)) {
			return p1;
		} else {
			return p1 + (((p2 - p1) / (v2 - v1)) * (demand - v1));
		}
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
