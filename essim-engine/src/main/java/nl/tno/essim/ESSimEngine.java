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

package nl.tno.essim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esdl.*;
import nl.tno.essim.grafana.GrafanaDashboardGenerator;
import nl.tno.essim.profiles.ProfileDataManager;
import nl.tno.essim.commons.CredentialUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.kpi.KPIManager;
import nl.tno.essim.kpi.data.KPIInfo;
import nl.tno.essim.loadflow.EssimLoadFlowController;
import nl.tno.essim.managers.EmissionManager;
import nl.tno.essim.managers.ObservationManager;
import nl.tno.essim.managers.SimulationManager;
import nl.tno.essim.model.*;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.mso.MSOClient;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.observation.ObservationField;
import nl.tno.essim.observation.consumers.InfluxDBObservationConsumer;
import nl.tno.essim.observation.consumers.KPIObservationConsumer;
import nl.tno.essim.observation.consumers.PostgresObservationConsumer;
import nl.tno.essim.observation.consumers.csv.CSVObservationConsumer;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.ChocoOptimiser;
import nl.tno.essim.transportsolver.TransportSolver;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ESSimEngine implements IStatusProvider {
    static {
        // Initialize the models
        EsdlPackage.eINSTANCE.eClass();
    }

    public static final EssimDuration SIMULATION_STEP = EssimDuration.of(1, ChronoUnit.HOURS);
    @Getter
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
//        objectMapper.
    }

    private final XMIResourceImpl esdlResource;
    private EssimLoadFlowController loadFlowController;
    private List<EnergyAsset> energyAssets;
    @Getter
    private String simulationId;
    private String scenarioName;
    private String energySystemId;
    @Getter
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
    private String databaseURL;
    private Date simRunTime;
    private List<NodeConfiguration> nodeConfig;
    private RemoteKPIModule kpiModules;
    private MSOConfiguration msoConfiguration;
    private double status;
    private String statusDescription = "";
    private String esdlString;
    private HashMap<Conversion, Solvers> convAssets;
    private IMongoBackend mongo;
    @Getter
    private ObservationManager observationManager;
    @Getter
    private Market market;

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

    public ESSimEngine(String essimId, String simulationId, EssimSimulation simulation, String esdl, IMongoBackend mongo)
            throws Exception {
        log.debug("ESSIM Revision: {}", essimId);
        this.mongo = mongo;
        simulationDescription = simulation.getSimulationDescription();
        user = simulation.getUser();
        simRunTime = simulation.getSimRunDate();
        nodeConfig = simulation.getNodeConfig();
        kpiModules = simulation.getKpiModule();
        msoConfiguration = simulation.getMso();
        if (simulation.getPostgresURL() != null && !simulation.getPostgresURL().isEmpty()) {
            databaseURL = simulation.getPostgresURL();
        } else {
            databaseURL = simulation.getInfluxURL();
        }

        scenarioName = simulation.getScenarioID();
        this.simulationId = simulationId;

        simulationStartTime = EssimTime.dateToLocalDateTime(simulation.getStartDate());
        simulationEndTime = EssimTime.dateToLocalDateTime(simulation.getEndDate());
        simulationStepLength = SIMULATION_STEP;
        String timeStep = simulation.getTimeStep();
        if (timeStep != null) {
            simulationStepLength = parseTimeStep(timeStep);
        }

        Market market = simulation.getMarket();
        // Set default min/max market prices if not defined by the input
        if (market == null) {
            market = new Market();
            market.setMinPrice(Commons.P_MIN);
            market.setMaxPrice(Commons.P_MAX);
            market.setDeltaPrice(Commons.MIN_DELTA_PRICE);
        }
        this.market = market;
        log.info("Using market price boundaries: [" + market.getMinPrice() + ", " + market.getMaxPrice() + "]");

        esdlString = esdl;
        ByteArrayInputStream esdlStream = new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8));
        esdlResource = ESDLUtil.loadEcoreResource(esdlStream);
        energySystem = (EnergySystem) esdlResource.getContents().getFirst();
        if (energySystem == null) {
            log.error("Failed to load Energy System");
            throw new IllegalStateException("Failed to load ESDL!");
        }


        // Initialise SimulationManager
        observationManager = new ObservationManager(simulationId);
        registerObservationConsumers(simulation);


        EssimTime simulationTime = new EssimTime(simulationStartTime, simulationEndTime, simulationStepLength);
        ProfileDataManager profileDataManager = new ProfileDataManager(esdlResource, simulationTime);
        simulationManager = new SimulationManager(simulationId, simulationTime, mongo, observationManager, profileDataManager);

        if (simulation.isPowsyblLoadFlow()) {
            loadFlowController = new EssimLoadFlowController(simulationId);
            simulationManager.setLoadFlowController(loadFlowController);
        }


        // Publish Simulation Description
        if (simulationDescription == null) {
            simulationDescription = "";
        }
        observationManager.publish(generalObservationProvider,
                Observation.builder()
                        .observedAt(simulationStartTime)
                        .value(ObservationField.SIM_DESCRIPTION, simulationDescription)
                        .build());

        energySystemId = "UnnamedEnergySystem";
        if (energySystem.getName() != null) {
            if (!energySystem.getName().isEmpty()) {
                energySystemId = energySystem.getName();
            }
        }

        log.debug("Successfully loaded Energy System {}", energySystemId);

        if (energySystem.getInstance() == null) {
            throw new IllegalArgumentException("No energy instance found in ESDL file!");
        }

        Instance instance = energySystem.getInstance().getFirst();
        if (instance.getArea() == null) {
            throw new IllegalArgumentException("No area found in ESDL energy instance !");
        }
        Area mainArea = instance.getArea();

        energyAssets = new ArrayList<EnergyAsset>();
        ESDLUtil.findAllEnergyAssets(mainArea, energyAssets);

        EnergySystemInformation esInformation = energySystem.getEnergySystemInformation();
        if (esInformation == null) {
            throw new IllegalArgumentException("No Energy System Information specified! Cannot read carrier information.");
        }
        Carriers carriers = esInformation.getCarriers();
        if (carriers == null) {
            throw new IllegalArgumentException("No Carrier Information specified!");

        }
        TreeMap<Integer, List<TransportSolver>> transportSolveOrder = createTransportSolvers(carriers);
        if (transportSolveOrder.isEmpty()) {
            throw new IllegalStateException("Conflicting Control Strategies found!!");
        }
        // Here we have all the information to build the powsybl network
        if (loadFlowController != null) {
            List<TransportSolver> electricityTransportSolvers = transportSolveOrder.entrySet().stream().flatMap(entry -> entry.getValue().stream()).filter(solver -> solver.getCarriers().stream().anyMatch(c -> c instanceof ElectricityCommodity)).toList();
            loadFlowController.createNetwork(electricityTransportSolvers, energyAssets);
        }


        log.debug("Going with this parallelised order:");

        // Add to simulation manager the transport solvers in the order we determined.
        for (int k : transportSolveOrder.keySet()) {
            List<TransportSolver> solvers = transportSolveOrder.get(k);
            log.debug(" - {}: {}", k, solvers.toString());
            for (TransportSolver solver : solvers) {
                solver.setSimulationManager(simulationManager);
                solver.setObservationManager(observationManager);
            }
            simulationManager.addSolvers(solvers);
        }
        EmissionManager emissionManager = EmissionManager.getInstance(simulationId);
        emissionManager.setObservationManager(observationManager);
        simulationManager.addOtherSimulatable(emissionManager);
        int numAssets = 0;
        for (TransportSolver solver : solversList) {
            numAssets += solver.getProcessedList().size();
        }
        int numSolvers = solversList.size();
        double totalSteps = 1.0 + ((double) (simulationEndTime.toEpochSecond(ZoneOffset.UTC)
                - simulationStartTime.toEpochSecond(ZoneOffset.UTC))) / simulationStepLength.getSeconds();
        double messages = totalSteps * (numAssets + numSolvers);

        if (kpiModules != null) {
            KPIManager.getInstance().getKPICommunicators().forEach((kpiCommunicator -> {
                KPIObservationConsumer kpiObservationConsumer = new KPIObservationConsumer(kpiCommunicator);
                kpiObservationConsumer.init(simulationId);
                observationManager.registerConsumer(kpiObservationConsumer);
                log.debug("Registering KPI Observation Consumer for {}", kpiCommunicator.getType());
                List<KPIModule> modules = simulation.getKpiModule().getModules();
                KPIInfo.KPIInfoBuilder kpiInfoBuilder = KPIInfo.builder().simulationId(simulationId).noOfMessages((long) messages);
                for (KPIModule module : modules) {
                    Object config = module.getConfig();
                    log.debug("Config for module {}: {}", module.getId(), config);
                    JsonNode jsonNodeConfig = objectMapper.valueToTree(config);
                    kpiInfoBuilder.kpiCalculator(new KPIInfo.KpiCalculator(module.getId(), jsonNodeConfig));
                }
                kpiCommunicator.sendKPIStart(kpiInfoBuilder.build());
            }));
        }

        if (msoConfiguration != null) {
            MSOClient msoClient = new MSOClient(essimId, simulationId, simulation.getMso(), nodeConfig, energySystem,
                    simulationManager);
            msoClient.deployModels();
        }

    }

    private void registerObservationConsumers(EssimSimulation simulation) {
        try {
            // Register all ObservationConsumers - Eg: InfluxDB
            if (simulation.getInfluxURL() != null) {
                InfluxDBObservationConsumer influxObservationConsumer = new InfluxDBObservationConsumer(
                        simulation.getInfluxURL());
                influxObservationConsumer.init(scenarioName);
                observationManager.registerConsumer(influxObservationConsumer);
                log.debug("Registering InfluxDB Observation Consumer: {}", scenarioName);
            }
            if (simulation.getCsvFilesLocation() != null) {
                CSVObservationConsumer csvObservationConsumer = new CSVObservationConsumer(
                        simulation.getCsvFilesLocation());
                observationManager.registerConsumer(csvObservationConsumer);
                csvObservationConsumer.init(scenarioName);
                log.debug("Registering CSV Observation Consumer");
            }
            if (simulation.getPostgresURL() != null) {
                String jdbcUri = "jdbc:" + simulation.getPostgresURL();
                CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(simulation.getPostgresURL());
                String username = credentials == null ? "essim" : credentials.username();
                String password = credentials == null ? "pgpassword" : credentials.password();
                PostgresObservationConsumer postgresObservationConsumer = new PostgresObservationConsumer(jdbcUri, username, password);
                observationManager.registerConsumer(postgresObservationConsumer);
                postgresObservationConsumer.init(scenarioName);
                log.debug("Registering Postgres Observation Consumer");
            }

        } catch (Exception e) {
//            simulationManager.shutdown();
            throw new IllegalArgumentException("Error in Observation Manager init", e);
        }
    }

    @NotNull
    private InfluxDBObservationConsumer getInfluxDBObservationConsumer(DatabaseConfiguration configuration) {
        String host = configuration.getHost();
        int port = configuration.getPort();
        boolean tls = configuration.isTls();
        if (port == 0) {
            port = tls ? 443 : 80;
        }
        String influxUrl = String.format("http%s://%s:%d", tls ? "s" : "", host, port);

        InfluxDBObservationConsumer influxObservationConsumer = new InfluxDBObservationConsumer(influxUrl);
        influxObservationConsumer.init(scenarioName);
        return influxObservationConsumer;
    }

    public TreeMap<Integer, List<TransportSolver>> createTransportSolvers(Carriers carriers) throws Exception {

        // Create all Transport Solvers based on carriers.
        // There could be isolated networks also. So repeat this process till we get a
        // TransportSolver for all networks.

        for (Carrier carrier : carriers.getCarrier()) {
            List<EnergyAsset> remainingAssets = new ArrayList<EnergyAsset>(energyAssets);
            int i = 0;
            List<EnergyAsset> filteredAssets = filterAssetsAndAssignRoles(remainingAssets, carrier);
            do {
                String solverId = energySystemId + " "
                        + (carrier.getName() == null ? carrier.getId() : carrier.getName()) + " Network " + i;
                TransportSolver solver = new TransportSolver(solverId, carrier, generalObservationProvider, nodeConfig,
                        energyAssetRoles, esdlString, this.market);
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
                                networkDiags.put(solverId, Base64.getEncoder().encodeToString(networkDiag.getBytes()));
                            } catch (Exception e) {
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

        // WHY? DISABLED TO REDUCE SIZE OF MONGO SIMULATION OBJECT
        // simulation.setTransport(getNetworkDiags());
        // MongoBackend.getInstance().updateSimulationData(simulationId, simulation);

        // Find all Conversion assets and their solver orders
        convAssets = findAllConversionAssets();

        // List<Integer[]> constrIndices = new ArrayList<Integer[]>();
        HashMap<String, List<Integer[]>> constrIndices = new HashMap<String, List<Integer[]>>();
        for (Conversion convAsset : convAssets.keySet()) {
            Solvers solverOrder = convAssets.get(convAsset);
            if (solverOrder.getFirst().isEmpty()) {
                continue;
            }
            for (TransportSolver first : solverOrder.getFirst()) {
                for (TransportSolver later : solverOrder.getLater()) {
                    if (constrIndices.get(convAsset.getName()) == null) {
                        constrIndices.put(convAsset.getName(), new ArrayList<Integer[]>());
                    }
                    List<Integer[]> constraintList = constrIndices.get(convAsset.getName());
                    constraintList.add(new Integer[]{solversList.indexOf(first), solversList.indexOf(later)});
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
        boolean emissionRow = false;
        EnergySystemInformation esi = energySystem.getEnergySystemInformation();
        if (esi != null) {
            Carriers carriers = esi.getCarriers();
            if (carriers != null) {
                for (Carrier carrier : carriers.getCarrier()) {
                    if (carrier instanceof EnergyCarrier) {
                        EnergyCarrier energyCarrier = (EnergyCarrier) carrier;
                        if (energyCarrier.getEmission() - 0.0 > Commons.eps) {
                            emissionRow = true;
                        }
                    }
                }
            }
        }
//        String databaseUrl = post
        GrafanaDashboardGenerator grafanaDashboardGenerator = new GrafanaDashboardGenerator(user, timeString, databaseURL, solversList, energySystemId,
                scenarioName, simulationId, simulationStartTime,
                simulationEndTime.plus(simulationStepLength.getAmount(), simulationStepLength.getUnit()), emissionRow, loadFlowController != null, mongo);
        return grafanaDashboardGenerator.getDashboardUrl();
    }

    private List<List<String>> printableSolversList(Solvers x) {
        List<String> firsts = printableSolverList(x.getFirst());
        List<String> laters = printableSolverList(x.getLater());

        List<List<String>> allSolvers = new ArrayList<List<String>>();
        allSolvers.add(firsts);
        allSolvers.add(laters);
        return allSolvers;
    }

    private List<String> printableSolverList(List<TransportSolver> x) {
        return x.stream().map(TransportSolver::getId).toList();
    }

    private List<EnergyAsset> filterAssetsAndAssignRoles(List<EnergyAsset> energyAssets, Carrier carrier) {
        List<EnergyAsset> filteredAssets = new ArrayList<EnergyAsset>();
        energyAssetRoles = new HashMap<EnergyAsset, Role>();

        for (EnergyAsset energyAsset : energyAssets) {
            for (Port port : energyAsset.getPort()) {
                Carrier portCarrier = port.getCarrier();
                if (portCarrier != null) {
                    if (portCarrier.getId().equals(carrier.getId()) && !filteredAssets.contains(energyAsset)) {
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
                                boolean portFound = conversion.getPort()
                                        .parallelStream()
                                        .anyMatch(p -> drivenByDemand.getOutPort().equals(p)
                                                && solver.getCarriers().contains(p.getCarrier()));
                                if (solver.getRole(conversion).equals(Role.PRODUCER) && portFound) {
                                    solvers.addFirst(solver);
                                } else {
                                    solvers.addLater(solver);
                                }
                            } else if (controlStrategy instanceof DrivenBySupply) {
                                DrivenBySupply drivenBySupply = (DrivenBySupply) controlStrategy;
                                boolean portFound = conversion.getPort()
                                        .parallelStream()
                                        .anyMatch(p -> drivenBySupply.getInPort().equals(p)
                                                && solver.getCarriers().contains(p.getCarrier()));
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
                                                + (conversion.getName() == null
                                                ? (" with ID " + conversion.getId())
                                                : (" with name " + conversion.getName()))
                                                + " has no control strategy defined!!");
                            } else {
                                log.warn(conversion.getClass().getInterfaces()[0].getSimpleName() + " asset "
                                        + (conversion.getName() == null
                                        ? (" with ID " + conversion.getId())
                                        : (" with name " + conversion.getName()))
                                        + " has no control strategy defined! Defaulting to DrivenByDemand.");
                                DrivenByDemand drivenByDemand = EsdlFactory.eINSTANCE.createDrivenByDemand();
                                drivenByDemand.setId(UUID.randomUUID().toString());
                                drivenByDemand.setName("DrivenByDemand for " + conversion.getName());
                                drivenByDemand.setOutPort(outport);
                                conversion.setControlStrategy(drivenByDemand);
                                Services services = energySystem.getServices();
                                if (services == null) {
                                    services = EsdlFactory.eINSTANCE.createServices();
                                }
                                EList<Service> serviceList = services.getService();
                                serviceList.add(drivenByDemand);

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

    public List<TransportNetwork> getNetworkDiags() {
        List<TransportNetwork> list = new ArrayList<TransportNetwork>();
        for (Entry<String, String> network : networkDiags.entrySet()) {
            TransportNetwork tn = new TransportNetwork();
            tn.setName(network.getKey());
            // tn.setNetworkHTMLDiag(Commons.compressString(network.getValue()));
            tn.setNetworkHTMLDiag(network.getValue());
            list.add(tn);
        }

        return list;
    }

    public static EssimDuration parseTimeStep(String timeStep) {
        String regex = "([0-9]+)([smhdMy]{1})";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(timeStep);
        if (matcher.matches()) {
            int amount = Integer.parseInt(matcher.group(1));
            ChronoUnit unit = null;
            switch (matcher.group(2)) {
                case "s":
                    unit = ChronoUnit.SECONDS;
                    break;
                case "m":
                    unit = ChronoUnit.MINUTES;
                    break;
                case "h":
                    unit = ChronoUnit.HOURS;
                    break;
                case "d":
                    unit = ChronoUnit.DAYS;
                    break;
                case "M":
                    unit = ChronoUnit.MONTHS;
                    break;
                case "y":
                    unit = ChronoUnit.YEARS;
                    break;
            }
            return EssimDuration.of(amount, unit);
        }
        log.error("Unsupported time step notation : {}. Defaulting to 1h.", timeStep);
        return SIMULATION_STEP;
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
