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
package nl.tno.essim;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import esdl.AbstractBuilding;
import esdl.Area;
import esdl.Asset;
import esdl.Carrier;
import esdl.Carriers;
import esdl.ControlStrategy;
import esdl.Conversion;
import esdl.CostInformation;
import esdl.DrivenByDemand;
import esdl.DrivenBySupply;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.EnergySystemInformation;
import esdl.EsdlFactory;
import esdl.EssimESDLPackage;
import esdl.GenericProfile;
import esdl.InPort;
import esdl.Instance;
import esdl.OutPort;
import esdl.Port;
import esdl.Transport;
import essim.EssimPackage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.grafana.GrafanaClient;
import nl.tno.essim.kpi.KPIModuleClient;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.managers.ObservationManager;
import nl.tno.essim.managers.SimulationManager;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.NodeConfiguration;
import nl.tno.essim.model.RemoteKPIModule;
import nl.tno.essim.model.TransportNetwork;
import nl.tno.essim.model.TransportNetworkImpl;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.observation.consumers.InfluxDBObservationConsumer;
import nl.tno.essim.observation.consumers.KafkaObservationConsumer;
import nl.tno.essim.observation.consumers.MQTTObservationConsumer;
import nl.tno.essim.observation.consumers.NATSObservationConsumer;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.transportsolver.ChocoOptimiser;
import nl.tno.essim.transportsolver.TransportSolver;

@Slf4j
public class ESSimEngine implements IStatusProvider {

	private static final EssimDuration SIMULATION_STEP = EssimDuration.of(1, ChronoUnit.HOURS);
	private List<EnergyAsset> energyAssets;
	@Getter
	private String simulationRunName;
	private String scenarioName;
	private String energySystemId;
	private EnergySystem energySystem;
	private LocalDateTime simulationStartTime;
	private LocalDateTime simulationEndTime;
	private EssimDuration simulationStepLength;
	private HashMap<EnergyAsset, Role> energyAssetRoles;
	private List<TransportSolver> solversList = new ArrayList<TransportSolver>();
	private String simulationDescription;
	private String user;
	@Getter
	private SimulationManager simulationManager;
	private HashMap<String, String> networkDiags = new HashMap<String, String>();
	private String influxURL;
	private Date simRunTime;
	private List<NodeConfiguration> nodeConfig;
	private RemoteKPIModule kpiModules;
	private double status;
	private String statusDescription = "";
	private HashMap<Conversion, Solvers> convAssets;

	private IObservationProvider generalObservationProvider = new IObservationProvider() {
		@Override
		public void setObservationManager(IObservationManager manager) {
			// TODO Auto-generated method stub
		}

		@Override
		public String getProviderType() {
			return "General";
		}

		@Override
		public String getProviderName() {
			return "ESSIM";
		}
	};

	public ESSimEngine(String simulationId, EssimSimulation simulation, File esdlFile) throws Exception {
		simulationDescription = simulation.getSimulationDescription();
		user = simulation.getUser();
		simRunTime = simulation.getSimRunDate();
		nodeConfig = simulation.getNodeConfig();
		kpiModules = simulation.getKpiModule();
		influxURL = simulation.getInfluxURL();

		scenarioName = simulation.getScenarioID();
		simulationRunName = simulationId;

		simulationStartTime = EssimTime.dateFromGUI(simulation.getStartDate());
		simulationEndTime = EssimTime.dateFromGUI(simulation.getEndDate());
		// TODO: Add this to ESSIM Configuration
		simulationStepLength = SIMULATION_STEP;

		// Initialise SimulationManager
		simulationManager = new SimulationManager(this, simulationId, simulationStartTime, simulationEndTime,
				simulationStepLength);

		// Initialise ObservationManager
		ObservationManager observationManager = new ObservationManager(simulationRunName);

		// Register all ObservationConsumers - Eg: InfluxDB
		if (simulation.getInfluxURL() != null) {
			InfluxDBObservationConsumer influxObservationConsumer = new InfluxDBObservationConsumer(
					simulation.getInfluxURL());
			influxObservationConsumer.init(scenarioName);
			observationManager.registerConsumer(influxObservationConsumer);
			simulationManager.addObservationConsumer(influxObservationConsumer);
			log.debug("Registering InfluxDB Observation Consumer");
		}
		if (simulation.getKafkaURL() != null) {
			KafkaObservationConsumer kafkaObservationConsumer = new KafkaObservationConsumer(simulation.getKafkaURL());
			kafkaObservationConsumer.init(simulationId);
			observationManager.registerConsumer(kafkaObservationConsumer);
			simulationManager.addObservationConsumer(kafkaObservationConsumer);
			log.debug("Registering Kafka Observation Consumer");
		}
		if (simulation.getNatsURL() != null) {
			NATSObservationConsumer natsObservationConsumer = new NATSObservationConsumer(simulation.getNatsURL());
			natsObservationConsumer.init(simulationId);
			observationManager.registerConsumer(natsObservationConsumer);
			simulationManager.addObservationConsumer(natsObservationConsumer);
			log.debug("Registering NATS Observation Consumer");
		}
		if (simulation.getMqttURL() != null) {
			MQTTObservationConsumer mqttObservationConsumer = new MQTTObservationConsumer(simulation.getMqttURL());
			mqttObservationConsumer.init(simulationId);
			observationManager.registerConsumer(mqttObservationConsumer);
			simulationManager.addObservationConsumer(mqttObservationConsumer);
			log.debug("Registering MQTT Observation Consumer");
		}

		// Publish Simulation Description
		if (simulationDescription != null) {
			observationManager.publish(generalObservationProvider, Observation.builder().observedAt(simulationStartTime)
					.value("SimDescription", simulationDescription).build());
		}

		energySystem = loadEcoreResource(esdlFile.getAbsoluteFile().toString());
		if (energySystem == null) {
			log.error("Failed to load Energy System from file {}", esdlFile);
			throw new IllegalStateException("Failed to load ESDL File!");
		}

		energySystemId = "UnnamedEnergySystem";
		if (energySystem.getName() != null) {
			if (!energySystem.getName().equals("")) {
				energySystemId = energySystem.getName();
			}
		}

		log.debug("Successfully loaded Energy System {}", energySystemId);

		if (energySystem.getInstance() == null) {
			throw new IllegalArgumentException("No energy instance found in ESDL file!");
		}
		// TODO: Figure out which instance to simulate. For now, take the first one.
		Instance instance = energySystem.getInstance().get(0);
		if (instance.getArea() == null) {
			throw new IllegalArgumentException("No area found in ESDL energy instance !");
		}
		Area mainArea = instance.getArea();

		energyAssets = new ArrayList<EnergyAsset>();
		findAllEnergyAssets(mainArea, energyAssets);

		// Calculate and publish CAPEX costs
		double[] capexCosts = calculateCAPEXCosts();
		observationManager.publish(generalObservationProvider, Observation.builder().observedAt(simulationStartTime)
				.value("investmentCosts", capexCosts[0]).value("installationCosts", capexCosts[1]).build());

		EnergySystemInformation esInformation = energySystem.getEnergySystemInformation();
		if (esInformation != null) {
			Carriers carriers = esInformation.getCarriers();
			if (carriers != null) {
				TreeMap<Integer, List<TransportSolver>> transportSolveOrder = determineTransportSolverOrder(carriers);
				if (transportSolveOrder.isEmpty()) {
					throw new IllegalStateException("Conflicting Control Strategies found!!");
				}

				log.debug("Going with this parallelised order:");

				// Add to simulation manager the transport solvers in the order we determined.
				for (int k : transportSolveOrder.keySet()) {
					List<TransportSolver> solvers = transportSolveOrder.get(k);
					log.debug(solvers.toString());
					simulationManager.addSolvers(solvers);
					for (TransportSolver solver : solvers) {
						solver.setSimulationManager(simulationManager);
						solver.setObservationManager(observationManager);
					}
				}

				EmissionManager emissionManager = EmissionManager.getInstance(simulationId);
				emissionManager.setObservationManager(observationManager);
				simulationManager.addOtherSimulatable(emissionManager);
			} else {
				// TODO: no carriers defined - going with copperplate
			}
		} else {
			// error
		}
		int numAssets = 0;
		for (TransportSolver solver : solversList) {
			numAssets += solver.getProcessedList().size();
		}
		int numSolvers = solversList.size();
		double totalSteps = 1.0 + ((double) (simulationEndTime.toEpochSecond(ZoneOffset.UTC)
				- simulationStartTime.toEpochSecond(ZoneOffset.UTC))) / simulationStepLength.getSeconds();
		double messages = totalSteps * (numAssets + numSolvers);

		if (kpiModules != null) {
			new KPIModuleClient(simulationId, simulation, messages);
		}

		/*
		 * String newFileName = energySystemFileName.split(".esdl")[0] + "_" +
		 * simulationEndTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".esdl";
		 * saveEcoreResource(energySystem, newFileName);
		 * 
		 * ESDLFile esdlFile = EssimFactory.eINSTANCE.createESDLFile();
		 * esdlFile.setId(energySystem.getId());
		 * esdlFile.setName(energySystem.getName()); esdlFile.setPath(newFileName);
		 * 
		 * ESSetup esSetup = EssimFactory.eINSTANCE.createESSetup();
		 * esSetup.setEnergySystemDescription(esdlFile);
		 * esSetup.setTimeStamp(DidoTime.localDateTimeToDate(simulationEndTime));
		 * simulationRun.setLatestESSetup(esSetup);
		 * 
		 * saveEcoreResource(essimProject,
		 * projectFileName.split(".essim")[0]+"_modified.essim");
		 */
	}

	public TreeMap<Integer, List<TransportSolver>> determineTransportSolverOrder(Carriers carriers) {

		// Create all Transport Solvers based on carriers.
		// There could be isolated networks also. So repeat this process till we get a
		// TransportSolver for all networks.

		for (Carrier carrier : carriers.getCarrier()) {
			List<EnergyAsset> remainingAssets = new ArrayList<EnergyAsset>(energyAssets);
			int i = 0;
			List<EnergyAsset> filteredAssets = filterAssetsByCarrier(remainingAssets, carrier);
			do {
				String solverId = energySystemId + " "
						+ (carrier.getName() == null ? carrier.getId() : carrier.getName()) + " Network " + i;
				TransportSolver solver = new TransportSolver(solverId, carrier, generalObservationProvider, nodeConfig,
						energyAssetRoles);
				solver.setSimulationManager(simulationManager);
				for (EnergyAsset filteredAsset : filteredAssets) {
					solver.addToNetwork(filteredAsset);
				}
				remainingAssets = solver.createTree();
				filteredAssets = remainingAssets;
				if (remainingAssets != null) {
					if (solver.getDeviceNodes().size() > 1) {
						String networkDiag = solver.getNetworkDiag();
						if (networkDiag != null) {
							try {
								networkDiags.put(solverId, URLEncoder.encode(networkDiag, "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								log.error("Error while creating network diagram for {}", solverId);
								networkDiags.put(solverId, "");
							}
						} else {
							log.debug("No network diagram for {}", solverId);
							networkDiags.put(solverId, "");
						}

						solversList.add(solver);
						i++;
					}
				} else {
					remainingAssets = new ArrayList<EnergyAsset>();
				}
			} while (!remainingAssets.isEmpty());
		}

		log.debug(solversList.toString());

		// Find all Conversion assets and their solver orders
		convAssets = findAllConversionAssets();

		List<Integer[]> constrIndices = new ArrayList<Integer[]>();
		for (Conversion convAsset : convAssets.keySet()) {
			Solvers solverOrder = convAssets.get(convAsset);
			if (solverOrder.getFirst().isEmpty()) {
				continue;
			}
			for (TransportSolver first : solverOrder.getFirst()) {
				for (TransportSolver later : solverOrder.getLater()) {
					constrIndices.add(new Integer[] { solversList.indexOf(first), solversList.indexOf(later) });
				}
			}
			log.debug(convAsset.getName() + " forces these orders: " + printableSolversList(solverOrder));
		}

		return new ChocoOptimiser(solversList, constrIndices).solve();
	}

	public void startSimulation() {
		simulationManager.startSimulation();
	}

	@Override
	public double getStatus() {
		if (simulationManager != null && simulationManager.isStarted()) {
			return simulationManager.getStatus();
		}
		return status;
	}

	@Override
	public String getDescription() {
		if (simulationManager != null && simulationManager.isStarted()) {
			return simulationManager.getDescription();
		}
		return statusDescription;
	}

	public String createGrafanaDashboard() {
		// Make a Grafana Dashboard:
		log.debug("Building Grafana Dashboard now!");
		if (user == null) {
			user = "ESSIM_USER";
		}
		String timeString;
		if (simRunTime == null) {
			timeString = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
		} else {
			timeString = DateTimeFormatter.ISO_DATE_TIME
					.format(LocalDateTime.ofInstant(simRunTime.toInstant(), ZoneId.of("UTC")));
		}
		GrafanaClient grafanaClient = new GrafanaClient(user, timeString, influxURL, solversList, energySystemId,
				scenarioName, simulationRunName, simulationStartTime, simulationEndTime);
		return grafanaClient.getDashboardUrl();
	}

	private double[] calculateCAPEXCosts() {
		double installationCosts = 0.0;
		double investmentCosts = 0.0;
		long simSeconds = Duration.between(simulationStartTime, simulationEndTime).getSeconds();
		Horizon simulationPeriod = new Horizon(simulationStartTime, EssimDuration.of(simSeconds, ChronoUnit.SECONDS));

		for (EnergyAsset asset : energyAssets) {
			CostInformation costInformation = asset.getCostInformation();
			if (costInformation != null) {
				GenericProfile installationCostProfile = costInformation.getInstallationCosts();
				if (installationCostProfile != null) {
					installationCosts += Commons.sum(Commons.readProfile(installationCostProfile, simulationPeriod));
				}
				GenericProfile investmentCostProfile = costInformation.getInvestmentCosts();
				if (investmentCostProfile != null) {
					investmentCosts += Commons.sum(Commons.readProfile(investmentCostProfile, simulationPeriod));
				}
			}
		}

		return new double[] { investmentCosts, installationCosts };
	}

	public void findAllEnergyAssets(Area area, List<EnergyAsset> assetList) {
		EList<Asset> areaAssets = area.getAsset();
		if (areaAssets != null) {
			for (Asset asset : areaAssets) {
				if (asset instanceof EnergyAsset) {
					EnergyAsset energyAsset = (EnergyAsset) asset;
					assetList.add(energyAsset);
				} else if (asset instanceof AbstractBuilding) {
					AbstractBuilding building = (AbstractBuilding) asset;
					findAllEnergyAssets(building, assetList);
				}
			}
		}
		EList<Area> subAreas = area.getArea();
		if (subAreas != null) {
			for (Area subArea : area.getArea()) {
				findAllEnergyAssets(subArea, assetList);
			}
		}
	}

	public void findAllEnergyAssets(AbstractBuilding building, List<EnergyAsset> assetList) {
		EList<Asset> buildingAssets = building.getAsset();
		if (buildingAssets != null) {
			for (Asset asset : buildingAssets) {
				if (asset instanceof EnergyAsset) {
					EnergyAsset energyAsset = (EnergyAsset) asset;
					assetList.add(energyAsset);
				} else if (asset instanceof AbstractBuilding) {
					AbstractBuilding subBuilding = (AbstractBuilding) asset;
					findAllEnergyAssets(subBuilding, assetList);
				}
			}
		}
	}

	public static <T extends EObject> T loadEcoreResource(File projectFile) throws IOException {
		return loadEcoreResource(projectFile.getPath());
	}

	public static <T extends EObject> T loadEcoreResource(String projectFileName) throws IOException {
		return loadEcoreResource(URI.createFileURI(projectFileName));
	}

	@SuppressWarnings("unchecked")
	public static <T extends EObject> T loadEcoreResource(URI projectFileURI) throws IOException {
		// Initialize the models
		EssimESDLPackage.eINSTANCE.eClass();
		EssimPackage.eINSTANCE.eClass();

		XMIResourceImpl resource = new XMIResourceImpl(projectFileURI);
		resource.getDefaultLoadOptions().put(XMIResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
		resource.setIntrinsicIDToEObjectMap(new HashMap<String, EObject>());
		resource.load(null);

		return (T) resource.getContents().get(0);
	}

	public static <T extends EObject> void saveEcoreResource(T ecoreResource, String fileName) throws IOException {
		XMIResource xmiResource = new XMIResourceImpl(URI.createURI(fileName));
		xmiResource.getContents().add(ecoreResource);
		HashMap<String, Object> opts = new HashMap<String, Object>();
		opts.put(XMIResource.OPTION_SCHEMA_LOCATION, true);
		xmiResource.save(opts);
	}

	// public static void main(String[] args) throws IOException {
	// ESSimEngine engine;
	// if (args.length > 0) {
	// engine = new ESSimEngine(args[0]);
	// } else {
	// engine = new ESSimEngine(PROJECT_FILE_NAME);
	// }
	// engine.createGrafanaDashboard();
	// }

	private List<List<String>> printableSolversList(Solvers x) {
		List<String> firsts = printableSolverList(x.getFirst());
		List<String> laters = printableSolverList(x.getLater());

		List<List<String>> allSolvers = new ArrayList<List<String>>();
		allSolvers.add(firsts);
		allSolvers.add(laters);
		return allSolvers;
	}

	private List<String> printableSolverList(List<TransportSolver> x) {
		return x.stream().map(y -> y.getId()).collect(Collectors.toList());
	}

	private List<EnergyAsset> filterAssetsByCarrier(List<EnergyAsset> energyAssets, Carrier carrier) {
		List<EnergyAsset> filteredAssets = new ArrayList<EnergyAsset>();
		energyAssetRoles = new HashMap<EnergyAsset, Role>();

		for (EnergyAsset energyAsset : energyAssets) {
			for (Port port : energyAsset.getPort()) {
				Carrier portCarrier = port.getCarrier();
				if (portCarrier != null) {
					if (portCarrier.getId().equals(carrier.getId())) {
						filteredAssets.add(energyAsset);
						if (energyAsset instanceof Transport) {
							energyAssetRoles.put(energyAsset, Role.TRANSPORT);
						} else {
							if (port instanceof InPort) {
								energyAssetRoles.put(energyAsset, Role.CONSUMER);
							} else {
								energyAssetRoles.put(energyAsset, Role.PRODUCER);
							}
						}
					}
				}
			}
		}

		return filteredAssets;
	}

	private HashMap<Conversion, Solvers> findAllConversionAssets() {
		HashMap<Conversion, Solvers> convAssets = new HashMap<Conversion, Solvers>();
		for (EnergyAsset energyAsset : energyAssets) {
			if (energyAsset instanceof Conversion) {
				Conversion conversion = (Conversion) energyAsset;
				Solvers solvers = new Solvers();
				// From all transport solvers
				for (TransportSolver solver : solversList) {
					// If this solver features this conversion asset
					if (solver.getProcessedList().contains(conversion)) {
						// What control strategy does it follow?
						// Driven by Demand : Put solver where Conversion is PRODUCER first, then input
						// solver(s)
						// Driven by Supply : Put solver where Conversion in CONSUMER first, then output
						// solver(s)
						// Driven by Profile : No particular ordering required
						ControlStrategy controlStrategy = conversion.getControlStrategy();
						if (controlStrategy != null) {
							// Control Strategy is specified - go according to specification
							if (controlStrategy instanceof DrivenByDemand) {
								DrivenByDemand drivenByDemand = (DrivenByDemand) controlStrategy;
								boolean portFound = conversion.getPort().parallelStream()
										.anyMatch(p -> drivenByDemand.getOutPort().equals(p)
												&& p.getCarrier().equals(solver.getCarrier()));
								if (solver.getRole(conversion).equals(Role.PRODUCER) && portFound) {
									solvers.addFirst(solver);
								} else {
									solvers.addLater(solver);
								}
							} else if (controlStrategy instanceof DrivenBySupply) {
								DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
								boolean portFound = conversion.getPort().parallelStream()
										.anyMatch(p -> drivenBySupply.getInPort().equals(p)
												&& p.getCarrier().equals(solver.getCarrier()));
								if (solver.getRole(conversion).equals(Role.CONSUMER) && portFound) {
									solvers.addFirst(solver);
								} else {
									solvers.addLater(solver);
								}
							}
						} else {
							// No control strategy specified. Default to Driven by Demand iff there is only
							// one outport
							// for this device. So CHPs and FuelCells must have explicit control strategies!
							OutPort outport = null;
							int i = 0;
							for (Port port : conversion.getPort()) {
								if (port instanceof OutPort) {
									outport = (OutPort) port;
									i++;
								}
							}
							if (i != 1) {
								throw new IllegalStateException(
										conversion.getClass().getInterfaces()[0].getSimpleName() + " asset "
												+ (conversion.getName() == null ? (" with ID " + conversion.getId())
														: (" with name " + conversion.getName()))
												+ " has no control strategy defined!!");
							} else {
								log.warn(conversion.getClass().getInterfaces()[0].getSimpleName() + " asset "
										+ (conversion.getName() == null ? (" with ID " + conversion.getId())
												: (" with name " + conversion.getName()))
										+ " has no control strategy defined! Defaulting to DrivenByDemand.");
								DrivenByDemand drivenByDemand = EsdlFactory.eINSTANCE.createDrivenByDemand();
								drivenByDemand.setOutPort(outport);
								conversion.setControlStrategy(drivenByDemand);
								if (solver.getRole(conversion).equals(Role.PRODUCER)) {
									solvers.addFirst(solver);
								} else {
									solvers.addLater(solver);
								}
							}
						}
					}
				}
				convAssets.put(conversion, solvers);
			}
		}
		return convAssets;
	}

	public void setFeatureCollections() {
		// log.debug("Going to persist geoJSON animation data..");
		// JSONObject featureCollection = new JSONObject();
		// featureCollection.put("type", "FeatureCollection");
		// JSONArray features = new JSONArray();
		// for (ITransportSolver solver : solversList) {
		// for (Object feature : solver.getFeatureCollection()) {
		// if (feature != null) {
		// features.put(feature);
		// }
		// }
		// }
		// featureCollection.put("features", features);
		// String originalFeatColl = featureCollection.toString();
		//
		// try (ZipOutputStream zos = new ZipOutputStream(new
		// FileOutputStream(simulationId + ".zip"))) {
		// ZipEntry zipEntry = new ZipEntry(simulationId + ".geojson");
		// zos.putNextEntry(zipEntry);
		// zos.write(originalFeatColl.getBytes("UTF-8"));
		// } catch (Exception e) {
		// log.error("Cannot write geojson information due to {}", e.getMessage());
		// }
		// log.debug("Done!");
	}

	public List<TransportNetwork> getNetworkDiags() {
		List<TransportNetwork> list = new ArrayList<TransportNetwork>();
		for (Entry<String, String> network : networkDiags.entrySet()) {
			TransportNetworkImpl tn = new TransportNetworkImpl();
			tn.setName(network.getKey());
			// tn.setNetworkHTMLDiag(Commons.compressString(network.getValue()));
			tn.setNetworkHTMLDiag(network.getValue());
			list.add(tn);
		}

		return list;
	}

}

class Solvers {
	@Getter
	private List<TransportSolver> first;
	@Getter
	private List<TransportSolver> later;

	public Solvers() {
		first = new ArrayList<TransportSolver>();
		later = new ArrayList<TransportSolver>();
	}

	public List<List<TransportSolver>> getOrder() {
		List<List<TransportSolver>> all = new ArrayList<List<TransportSolver>>();
		all.add(first);
		all.add(later);
		return all;
	}

	public void addFirst(TransportSolver solver) {
		first.add(solver);
	}

	public void addLater(TransportSolver solver) {
		later.add(solver);
	}

	public void addFirst(List<TransportSolver> solvers) {
		first.addAll(solvers);
	}

	public void addLater(List<TransportSolver> solvers) {
		later.addAll(solvers);
	}
}