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

package nl.tno.essim.transportsolver;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.json.JSONArray;

import esdl.Asset;
import esdl.Carrier;
import esdl.Consumer;
import esdl.Conversion;
import esdl.DrivenByDemand;
import esdl.DrivenByProfile;
import esdl.EnergyAsset;
import esdl.EsdlFactory;
import esdl.GenericProfile;
import esdl.InPort;
import esdl.OutPort;
import esdl.Port;
import esdl.Producer;
import esdl.ProfileReference;
import esdl.Transport;
import essim.ESSIMDateTimeProfile;
import essim.ESSIMInfluxDBProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.commons.ISimulationManager;
import nl.tno.essim.commons.ITransportSolver;
import nl.tno.essim.commons.Simulatable;
import nl.tno.essim.commons.SimulationStatus;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.transportsolver.nodes.Node;
import nl.tno.essim.transportsolver.nodes.Node.NodeBuilder;
import nl.tno.essim.util.Converter;

@Slf4j
public class TransportSolver implements ITransportSolver, Simulatable, IObservationProvider {
	private static final String D3_TREE_PAGE_OUTPUT_FILE = "tree";
	private static final String D3_TREE_PAGE_TEMPLATE = "tree/tree.html.template";
	private static final String D3_TREEDATA_VAR_PLACEHOLDER = "$$TREEDATA$$";
	private static final String D3_HEADER_PLACEHOLDER = "$$HEADER$$";
	@Getter
	private String id;
	@Getter
	private Carrier carrier;
	private List<EnergyAsset> assetList;
	@Getter
	private List<EnergyAsset> processedList;
	@Getter
	private List<Node> deviceNodes;
	@Getter
	private String networkDiag;
	private IObservationManager observationManager;
	@Getter
	private Node tree;
	private LocalDateTime simulationStartTime;
	private LocalDateTime simulationEndTime;
	private EssimDuration simulationStepLength;
	private HashMap<EnergyAsset, Role> roleMap;
	private int transportCount;
	private Collection<NodeConfiguration> nodeConfig;
	private String simulationId;

	public TransportSolver(String name, Carrier carrier, IObservationProvider generalObservationProvider,
			Collection<NodeConfiguration> nodeConfig, HashMap<EnergyAsset, Role> roleMap) {
		this.id = name;
		this.carrier = carrier;
		this.nodeConfig = nodeConfig;
		assetList = new ArrayList<EnergyAsset>();
		processedList = new ArrayList<EnergyAsset>();
		deviceNodes = new ArrayList<Node>();
		this.roleMap = roleMap;
	}

	public boolean isPartOfNetwork(Asset asset) {
		if (asset instanceof EnergyAsset) {
			EnergyAsset energyAsset = (EnergyAsset) asset;
			for (Port port : energyAsset.getPort()) {
				if (carrier.equals(port.getCarrier())) {
					return true;
				}
			}
		}
		return false;
	}

	public void addToNetwork(EnergyAsset asset) {
		if (!assetList.contains(asset)) {
			if (asset instanceof Conversion && asset.getControlStrategy() == null) {
				log.warn("Conversion asset {} has no Control Strategy. Defaulting to DrivenByDemand",
						asset.getName() == null ? asset.getId() : asset.getName());
				DrivenByDemand drivenByDemand = EsdlFactory.eINSTANCE.createDrivenByDemand();
				OutPort outport = null;
				for (Port port : asset.getPort()) {
					if (port instanceof OutPort) {
						outport = (OutPort) port;
						break;
					}
				}
				drivenByDemand.setOutPort(outport);
				asset.setControlStrategy(drivenByDemand);
			}
			assetList.add(asset);
		}
	}

	public Role getRole(EnergyAsset asset) {
		return getRole(tree, asset);
	}

	private Role getRole(Node node, EnergyAsset asset) {
		if (node.getAsset().equals(asset)) {
			return node.getRole();
		}
		if (node.getChildren() != null) {
			for (Node childNode : node.getChildren()) {
				Role role = getRole(childNode, asset);
				if (role != null) {
					return role;
				}
			}
		}

		return null;
	}

	public List<EnergyAsset> createTree() {
		EnergyAsset rootAsset = null;
		double maxCapacity = Double.NEGATIVE_INFINITY;
		Role rootRole = Role.TRANSPORT;

		if (assetList.size() <= 1) {
			return null;
		}

		// Choose the biggest producer as the root node
		for (EnergyAsset asset : assetList) {
			if (asset instanceof Producer) {
				Producer producer = (Producer) asset;
				if (producer.getPower() > maxCapacity) {
					rootAsset = producer;
					maxCapacity = producer.getPower();
				}
			} else if (asset instanceof Conversion && roleMap.get(asset).equals(Role.PRODUCER)) {
				Conversion conversion = (Conversion) asset;
				if (conversion.getPower() > maxCapacity) {
					rootAsset = conversion;
					maxCapacity = conversion.getPower();
				}
			} else if (asset instanceof Consumer && roleMap.get(asset).equals(Role.PRODUCER)) {
				Consumer consumer = (Consumer) asset;
				if (consumer.getPower() > maxCapacity) {
					rootAsset = consumer;
					maxCapacity = consumer.getPower();
				}
			}
		}

		rootRole = roleMap.get(rootAsset);

		tree = Node.builder().nodeId(rootAsset.getName() == null ? rootAsset.getId() : rootAsset.getName())
				.simulationId(simulationId).asset(rootAsset).role(rootRole).parent(null).networkId(getId())
				.carrier(carrier).build();

		processedList.add(rootAsset);
		assetList.remove(rootAsset);
		if (rootAsset instanceof Transport) {
			transportCount += 1;
		}
		makeTree(tree);
		printTree(getId());

		tree.findDeviceNodes(deviceNodes);

		return assetList;
	}

	public void makeTree(Node parentNode) {
		// if (!(Commons.isConversionToSameCarrier(parentNode.getAsset()))) {
		for (EnergyAsset connectedAsset : Commons.findAllConnectedAssets(parentNode.getAsset())) {

			if (assetList.contains(connectedAsset) && !processedList.contains(connectedAsset)) {
				final String nodeId = connectedAsset.getId();
				NodeBuilder nodeBuilder = Node.builder().nodeId(nodeId).simulationId(simulationId).asset(connectedAsset)
						.parent(parentNode).networkId(getId()).carrier(carrier);
				for (Port myPort : connectedAsset.getPort()) {
					if (myPort instanceof InPort) {
						InPort myInPort = (InPort) myPort;
						for (OutPort outPort : myInPort.getConnectedTo()) {
							if (outPort.getEnergyasset().equals(parentNode.getAsset())) {
								nodeBuilder.directionFactor(1);
								break;
							}
						}
					} else {
						OutPort myOutPort = (OutPort) myPort;
						for (InPort inPort : myOutPort.getConnectedTo()) {
							if (inPort.getEnergyasset().equals(parentNode.getAsset())) {
								nodeBuilder.directionFactor(-1);
								break;
							}
						}
					}
				}

				if (roleMap.containsKey(connectedAsset)) {
					nodeBuilder.role(roleMap.get(connectedAsset));
				} else {
					nodeBuilder.role(Role.PRODUCER);
				}

				if (!Commons.isConversionToSameCarrier(connectedAsset)) {
					processedList.add(connectedAsset);
					assetList.remove(connectedAsset);
				} else {
					if (connectedVia(connectedAsset, parentNode.getAsset()) instanceof InPort) {
						nodeBuilder.role(Role.CONSUMER);
						roleMap.put(connectedAsset, Role.PRODUCER);
					} else {
						nodeBuilder.role(Role.PRODUCER);
						roleMap.put(connectedAsset, Role.CONSUMER);
					}
				}
				if (connectedAsset instanceof Transport) {
					transportCount += 1;
				}

				if (nodeConfig != null) {
					this.nodeConfig.stream().filter(n -> nodeId.equals(n.getEsdlNodeId())).findFirst()
							.ifPresent(nodeBuilder::config);
				}

				parentNode.addChild(nodeBuilder.build());
			}
		}
		// }
		if (parentNode.getChildren() != null) {
			for (Node childNode : parentNode.getChildren()) {
				if (!(Commons.isConversionToSameCarrier(childNode.getAsset()))) {
					makeTree(childNode);
				} else {
					processedList.add(childNode.getAsset());
				}
			}
		}
	}

	private Port connectedVia(EnergyAsset from, EnergyAsset to) {
		Port port = null;

		for (Port fromPort : from.getPort()) {
			if (fromPort instanceof InPort) {
				InPort inPort = (InPort) fromPort;
				for (OutPort outPort : inPort.getConnectedTo()) {
					if (outPort.getEnergyasset().equals(to)) {
						return inPort;
					}
				}
			} else {
				OutPort outPort = (OutPort) fromPort;
				for (InPort inPort : outPort.getConnectedTo()) {
					if (inPort.getEnergyasset().equals(to)) {
						return outPort;
					}
				}
			}
		}

		return port;
	}

	public void printTree(String diff) {
		try {
			String template = Commons.readFileIntoString(D3_TREE_PAGE_TEMPLATE);
			if (template == null) {
				log.error("Could not read TREE template located at {}!", D3_TREE_PAGE_TEMPLATE);
				return;
			}

			JSONArray jsonTree = new JSONArray().put(tree.getJSONString());
			String outString = template.replace(D3_TREEDATA_VAR_PLACEHOLDER,
					"var treeData=" + jsonTree.toString() + ";");
			networkDiag = outString.replace(D3_HEADER_PLACEHOLDER, diff);
			String fileName = D3_TREE_PAGE_OUTPUT_FILE + "_" + diff.replace(' ', '_') + ".html";
			PrintWriter out = new PrintWriter(fileName);
			out.println(networkDiag);
			out.close();

			log.debug("Printed tree in {}", fileName);
		} catch (Exception e) {
			log.warn("Could not print the tree.");
		}
	}

	@Override
	public void init(EssimTime timestamp) {
		simulationStartTime = timestamp.getSimulationStartTime();
		simulationEndTime = timestamp.getSimulationEndTime();
		simulationStepLength = timestamp.getSimulationStepLength();

		if (tree == null) {
			throw new IllegalStateException("TransportSolver " + getId() + " in init() without creating tree!");
		}

		for (EnergyAsset asset : deviceNodes.stream().map(x -> x.getAsset()).collect(Collectors.toList())) {
			for (Port port : asset.getPort()) {
				initialiseProfile(port);
			}

			if (asset.getControlStrategy() != null) {
				if (asset.getControlStrategy() instanceof DrivenByProfile) {
					initialiseProfile(((DrivenByProfile) asset.getControlStrategy()).getProfile());
				}
			}
		}
	}

	@Override
	public void step(EssimTime timestamp) {

		log.trace("{} received heartbeat : {}", getId(), timestamp.getTime());

		EssimDuration timeStepinDT = timestamp.getSimulationStepLength();
		long timeStep = timeStepinDT.getSeconds();
		Horizon now = new Horizon(timestamp.getTime(), timeStepinDT);

		// Create Bid Curves.
		for (Node deviceNode : deviceNodes) {
			deviceNode.createBidCurve(timeStep, now, Commons.P_MIN, Commons.P_MAX);
		}

		// Prices are between 0 and 1, but O&M costs may not be. First normalise these
		// costs.
		// tree.normaliseCosts();

		// Send demand functions upwards
		TreeMap<Double, Double> summedFunction = tree.aggregateDemandFunction();

		// Allocate devices and collect observations
		ArrayList<Observation> observations = new ArrayList<Observation>();
		double[] results = tree.allocateAndPropagate(summedFunction, observations, timestamp);

		// Publish observations
		if (observationManager != null) {
			observationManager.publish(this,
					Observation.builder().observedAt(timestamp.getTime()).tag("transportNetworkId", getId())
							.tag("carrierId", carrier.getId())
							.tag("carrierName", (carrier.getName() == null) ? "UnnamedCarrier" : carrier.getName())
							.value("imbalanceEnergy", results[0]).value("matchingPrice", results[1])
							.value("imbalancePower", results[0] / timeStep).build());

			for (Observation observation : observations) {
				observationManager.publish(this, observation);
			}
		}

		EmissionManager.getInstance(simulationId).organiseNetwork(getId());
	}

	@Override
	public void stop() {
		for (EnergyAsset asset : deviceNodes.stream().map(x -> x.getAsset()).collect(Collectors.toList())) {
			for (Port port : asset.getPort()) {
				for (GenericProfile profile : port.getProfile()) {
					if (profile != null) {
						if (profile instanceof ProfileReference) {
							profile = ((ProfileReference) profile).getReference();
						}
						if (profile instanceof ESSIMInfluxDBProfile) {
							ESSIMInfluxDBProfile profileImpl = (ESSIMInfluxDBProfile) profile;
							profileImpl.initProfile(null, null, null);
						} else if (profile instanceof ESSIMDateTimeProfile) {
							ESSIMDateTimeProfile dtProfile = (ESSIMDateTimeProfile) profile;
							dtProfile.initProfile(null, null, null);
						}
					}
				}
			}
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSimulationManager(ISimulationManager manager) {
		this.simulationId = manager.getName();
	}

	@Override
	public void setObservationManager(IObservationManager manager) {
		this.observationManager = manager;
	}

	@Override
	public String getProviderName() {
		return getId();
	}

	@Override
	public String getProviderType() {
		return "TransportSolver";
	}

	@Override
	public String toString() {
		return getId();
	}

	private void initialiseProfile(Port port) {
		for (GenericProfile profile : port.getProfile()) {
			initialiseProfile(profile);
		}
	}

	private void initialiseProfile(GenericProfile profile) {
		if (profile != null) {
			if (profile instanceof ProfileReference) {
				profile = ((ProfileReference) profile).getReference();
			}
			if (profile instanceof ESSIMInfluxDBProfile) {
				ESSIMInfluxDBProfile profileImpl = (ESSIMInfluxDBProfile) profile;
				profileImpl.initProfile(EssimTime.localDateTimeToDate(simulationStartTime),
						EssimTime.localDateTimeToDate(simulationEndTime),
						Converter.toESDLDuration(simulationStepLength));
			} else if (profile instanceof ESSIMDateTimeProfile) {
				ESSIMDateTimeProfile dtProfile = (ESSIMDateTimeProfile) profile;
				dtProfile.initProfile(EssimTime.localDateTimeToDate(simulationStartTime),
						EssimTime.localDateTimeToDate(simulationEndTime),
						Converter.toESDLDuration(simulationStepLength));
			}
		}
	}

	@Override
	public JSONArray getFeatureCollection() {
		return tree.getGeoJsonFeatures();
	}

	@Override
	public boolean hasAnyTransportAsset() {
		return transportCount >= 1;
	}

	@Override
	public SimulationStatus getState() {
		// TODO Auto-generated method stub
		return null;
	}
}
