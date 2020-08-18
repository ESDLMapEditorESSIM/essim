package nl.tno.essim.transportsolver.nodes;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.CurtailmentStrategy;
import esdl.EnergyAsset;
import esdl.GenericProfile;
import esdl.OutPort;
import esdl.PVPanel;
import esdl.Port;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
public class PVPanelNode extends ProducerNode {

	private PVPanel pvPanel;
	private double power;
	private double panelEfficiency;

	@Builder(builderMethodName = "pVPanelNodeBuilder")
	public PVPanelNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray,
			JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.pvPanel = (PVPanel) asset;
		this.power = pvPanel.getPower();
		this.panelEfficiency = pvPanel.getPanelEfficiency() < Commons.eps ? 1.0 : pvPanel.getPanelEfficiency();
		// TODO: Selma
		// Curtailment strategy check
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
		double energyOutput = Double.NaN;

		// Checks if an asset is operational (accounts for Commissioning and
		// Decommissioning date)
		if (!isOperational(now)) {
			makeInflexibleConsumptionFunction(0);
			return;
		}
		for (Port port : pvPanel.getPort()) {
			if (port instanceof OutPort) {
				GenericProfile profile = Commons.getEnergyProfile(port);
				if (profile != null) {
					if (Commons.isPowerProfile(profile)) {
						energyOutput = timeStep * Commons.aggregatePower(Commons.readProfile(profile, now));
						break;
					} else if (Commons.isEnergyProfile(profile)) {
						energyOutput = Commons.aggregateEnergy(Commons.readProfile(profile, now));
						break;
					}
				}
			}
		}
		if (Double.isNaN(energyOutput)) {
			energyOutput = timeStep * power * panelEfficiency;
		}
		// Curtailment strategy implementation
		if (controlStrategy != null && controlStrategy instanceof CurtailmentStrategy) {		
			CurtailmentStrategy curtailmentStrategy = (CurtailmentStrategy) controlStrategy;
			energyOutput = Math.min(energyOutput, curtailmentStrategy.getMaxPower() * timeStep);
		}

		// Participation in the simulation
		makeInflexibleProductionFunction(energyOutput);
	}

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		super.processAllocation(timestamp, builder, price);
	}

}
