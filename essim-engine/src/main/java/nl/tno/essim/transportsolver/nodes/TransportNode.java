package nl.tno.essim.transportsolver.nodes;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import nl.tno.essim.observation.Observation.ObservationBuilder;
import esdl.Carrier;
import esdl.EnergyAsset;
import esdl.Transport;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransportNode extends Node {

	private Transport transport;
	private double capacity;

	@Builder(builderMethodName = "transportNodeBuilder")
	public TransportNode(String simulationId, String nodeId, String address, String networkId, JSONArray animationArray,
			JSONObject geoJSON, EnergyAsset asset, int directionFactor, Role role,
			TreeMap<Double, Double> demandFunction, double energy, double cost, Node parent, Carrier carrier,
			List<Node> children, long timeStep, Horizon now) {
		super(simulationId, nodeId, address, networkId, animationArray, geoJSON, asset, directionFactor, role,
				demandFunction, energy, cost, parent, carrier, children, timeStep, now);
		this.transport = (Transport) asset;
		this.capacity = transport.getCapacity();
		// this.geoJSON = new JSONObject(
		// "{\"type\":\"Feature\",\"properties\":{\"time\":1430391600000,\"id\":2,\"stroke\":\"#ff0000\",\"strokewidth\":2},\"geometry\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiLineString\",\"coordinates\":[]}]}}");
		// Geometry assetGeometry = asset.getGeometry();
		// JSONArray coordinates = new JSONArray();
		// if (assetGeometry instanceof Line) {
		// JSONArray linePoints = new JSONArray();
		// Line line = (Line) assetGeometry;
		// for (Point point : line.getPoint()) {
		// JSONArray pointLatLong = new JSONArray();
		// pointLatLong.put(point.getLon());
		// pointLatLong.put(point.getLat());
		// linePoints.put(pointLatLong);
		// }
		// coordinates.put(linePoints);
		// }
		// this.geoJSON.getJSONObject("geometry")
		// .getJSONArray("geometries")
		// .getJSONObject(0)
		// .put("coordinates", coordinates);
		// this.animationArray = new JSONArray();
	}

	@Override
	public void createBidCurve(long timeStep, Horizon now, double minPrice, double maxPrice) {
	};

	@Override
	public void processAllocation(EssimTime timestamp, ObservationBuilder builder, double price) {
		long timeStepInSeconds = timestamp.getSimulationStepLength()
				.getSeconds();
		builder.tag("capability", "Transport");
		builder.value("capacity", capacity)
		.value("allocationEnergy", directionFactor * energy)
		.value("allocationPower", directionFactor * energy / timeStepInSeconds);
		if (capacity != 0) {
			double load = directionFactor * energy / (timeStepInSeconds * capacity);
			builder.value("load", load);			

//			JSONObject properties = geoJSON.getJSONObject("properties");
//			properties.put("strokewidth", Math.round(Commons.getLoadWidth(load)));
//			properties.put("stroke", Commons.getLoadColour(load));
//			properties.put("time", timestamp.getTime()
//					.toInstant(ZoneOffset.UTC)
//					.toEpochMilli());
//			properties.put("id", SequentialID.getInstance()
//					.getNewIndex());
//			JSONObject copy = new JSONObject(geoJSON.toString());
//			animationArray.put(copy);
		}
	}

}
