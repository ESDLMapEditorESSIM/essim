package nl.tno.essim.transportsolver.nodes;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.PowerPlant;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

public class PowerPlantNode extends ConversionNode {

	protected PowerPlant powerPlant;
	protected GenericProfile mustRunProfile;

	public PowerPlantNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray, JSONObject geoJSON,
			EnergyAsset asset, int directionFactor, Role role, TreeMap<Double, Double> demandFunction, double energy,
			double cost, Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role, demandFunction, energy, cost,
				parent, carrier, children, timeStep, now);
		this.powerPlant = (PowerPlant) asset;
		this.mustRunProfile = powerPlant.getMustRun();
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		super.createBidCurve(timeStep, now, minPrice, maxPrice);
		// TODO: Something to do with the mustRun Profile
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		super.processAllocation(timestamp, builder, price);
	}

}
