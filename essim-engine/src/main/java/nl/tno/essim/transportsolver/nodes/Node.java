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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import esdl.AbstractBuilding;
import esdl.Area;
import esdl.Carrier;
import esdl.Conversion;
import esdl.EnergyAsset;
import esdl.Port;
import esdl.Sector;
import esdl.Transport;
import esdl.impl.ItemImpl;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.observation.Observation.ObservationBuilder;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@Data
@Slf4j
public abstract class Node implements INode {
    private static boolean storeBidCurves = System.getenv().containsKey("STORE_BIDCURVES");

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
    protected Port connectedPort;

    /** these are set by {@link #createBidCurve(long, Horizon, Market)} **/
    private double minMarketPrice;
    private double maxMarketPrice;
    private double deltaPrice;

    @Builder
    public Node(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
        super();
        this.simulationId = simulationId;
        this.nodeId = nodeId;
        this.address = address;
        this.networkId = networkId;
        this.asset = asset;
        this.esdlString = esdlString;
        this.directionFactor = directionFactor;
        this.role = role;
        this.demandFunction = demandFunction;
        this.energy = energy;
        this.cost = cost;
        this.parent = parent;
        this.carrier = carrier;
        this.children = children;
        this.timeStep = timeStep;
        this.now = now;
        this.connectedPort = connectedPort;
        this.minMarketPrice = 0.0;
        this.maxMarketPrice = 1.0;
        this.deltaPrice = 1E-6;
    }

    public double getDefaultMarginalCost() {
        return (maxMarketPrice - minMarketPrice) / 2.0;
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
            BidFunction aggregatedFunction = null;
            ;
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
                demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice,
                        new double[][]{{minMarketPrice, 0.0}, {maxMarketPrice, 0.0}});
                setDemandFunction(demandFunction);
            }
            return demandFunction;
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

        return new double[]{min, max};
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
//		log.trace("Market {} - summed function: {}", getNetworkId(), summedFunction);
//		log.trace("Balancing price: {}, imbalance: {}", balancingPrice, imbalance);
        // Propagate
        propagate(balancingPrice, observations, timestamp);

        double[] vals = {imbalance, balancingPrice};
        return vals;
    }

    public JSONObject getJSONString() {
        String ctrlStrategy = "";
        if (getAsset() instanceof Conversion) {
            if (getAsset().getControlStrategy() != null) {
                ctrlStrategy = "[" + getAsset().getControlStrategy().getClass().getInterfaces()[0].getSimpleName()
                        + "]";
            }
        }
        JSONObject nodeObj = new JSONObject().put("name", getNodeId() + "(" + getRole() + ")" + ctrlStrategy);
        if (parent != null) {
            nodeObj.put("parent", parent.getNodeId() + "(" + parent.getRole() + ")" + ctrlStrategy);
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

        demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice,
                new double[][]{{minMarketPrice, energy}, {maxMarketPrice, energy}});
        demandFunction.setMarginalCost(0.0); // no marginal cost to produce?
    }

    public void makeInflexibleConsumptionFunction(double emax) {
        energy = emax;

        demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice,
                new double[][]{{minMarketPrice, energy}, {maxMarketPrice, energy}});
        demandFunction.setMarginalCost(0.0); // no marginal cost to consume?
    }

    public void makeAdjustableProductionFunction(double emax) {
        energy = -emax;

        demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice, new double[][]{
                {minMarketPrice, 0.0}, {cost, 0.0}, {cost + deltaPrice, energy}, {maxMarketPrice, energy}});
        demandFunction.setMarginalCost(cost);
    }

    public void makeAdjustableConsumptionFunction(double emax) {
        energy = emax;
        demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice, new double[][]{
                {minMarketPrice, energy}, {cost, energy}, {cost + deltaPrice, 0}, {maxMarketPrice, 0}});
        demandFunction.setMarginalCost(cost);
//		demandFunction.addPoint(minMarketPrice, energy);
//		demandFunction.addPoint(0.0, energy);
//		if (cost > 0.0) { // TODO make this work for negative mc
//			demandFunction.addPoint(cost, 0.0);
//		}
//		demandFunction.addPoint(cost, energy);
//		demandFunction.addPoint(maxMarketPrice, 0.0);
    }

    public void makeStorageFunction(double ecmax, double edmax, double soc, Double mcCharge, Double mcDischarge) {

        // Marginal cost of storage moves inversely as SOC.
        // Higher the SOC, the more eager it is to start producing.
        // cost = 0.9 - 0.8 * soc;
        // cost = 1 - 0.9 * soc * soc;
        // cost = Math.max(0, Math.min(1, 1 - soc));
        double delta = deltaPrice;
        cost = 0.15 * (maxMarketPrice - minMarketPrice);
        if (mcCharge == null || Double.isNaN(mcCharge)) {
            mcCharge = Math.max(0.0, 0.95 * cost);
        }
        if (mcDischarge == null || Double.isNaN(mcDischarge)) {
            mcDischarge = Math.min(1.0, 1.05 * cost);
        }

        demandFunction = new BidFunction(minMarketPrice, maxMarketPrice, deltaPrice,
                new double[][]{{minMarketPrice, ecmax},
                        {Math.max(minMarketPrice, Math.min(mcCharge, maxMarketPrice)), ecmax},
                        {Math.max(minMarketPrice, Math.min(mcCharge + delta, maxMarketPrice)), 0.0},
                        {Math.max(minMarketPrice, Math.min(mcDischarge, maxMarketPrice)), 0.0},
                        {Math.max(minMarketPrice, Math.min(mcDischarge + delta, maxMarketPrice)), -edmax},
                        {maxMarketPrice, -edmax}});
        demandFunction.setMarginalCost(mcCharge);
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
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        }
        return BidFunction.sumCurves(a, b);
    }

    private void propagate(double price, List<Observation> observations, EssimTime timestamp) {
        // Allocate
        energy = findDemandFromCurve(demandFunction, price);

        // print curve to stdout
        // log.trace("{} - {}: {}, price={}, energy={}", timestamp.getTime(),
        // getAsset().getName(), demandFunction.toString(), price, energy);

        // Make observation
        ObservationBuilder builder = Observation.builder().observedAt(timestamp.getTime()).tag("assetId", asset.getId())
                .tag("assetName", asset.getName() == null ? "UnnamedAsset" : asset.getName())
                .tag("assetClass", asset.getClass().getInterfaces()[0].getSimpleName()).tag("address", address)
                .tag("carrierId", carrier.getId()).tag("role", role.name())
                .tag("carrierName", carrier.getName() == null ? "UnnamedCarrier" : carrier.getName())
                .value("allocationEnergy", energy)
                .value("allocationPower", energy / timestamp.getSimulationStepLength().getSeconds())
                .value("marginalCost", demandFunction.getMarginalCost());

        if (storeBidCurves) {
            builder.value("bidcurve", this.demandFunction);
        }

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
    public void createBidCurve(long timeStep, Horizon now, Market marketInfo) {
        if (this.timeStep == 0l) {
            this.timeStep = timeStep;
        }
        if (this.now == null) {
            this.now = now;
        }
        updateMarketInfo(marketInfo);
    }

    public void updateMarketInfo(Market marketInfo) {
        this.minMarketPrice = marketInfo.getMinPrice().doubleValue();
        this.maxMarketPrice = marketInfo.getMaxPrice().doubleValue();
        this.deltaPrice = marketInfo.getDeltaPrice().doubleValue();
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

        public Node build() throws Exception {
            Node node = null;
            if (this.config != null) {
                node = new RemoteLogicNode(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor,
                        role, demandFunction, energy, cost, parent, carrier, children, timeStep, now, config,
                        connectedPort);
            } else if (asset != null) {
                Class<?> assetNodeClass = null;
                classSearch:
                for (Class<?> clazz = asset.getClass(); !clazz.equals(ItemImpl.class); clazz = clazz
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
                                Horizon.class, Port.class).newInstance(simulationId, nodeId, address, networkId, asset,
                                esdlString, directionFactor, role, demandFunction, energy, cost, parent,
                                carrier, children, timeStep, now, connectedPort);
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

}
