package nl.tno.essim.loadflow;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.serde.XMLExporter;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.ac.solver.NewtonRaphsonFactory;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.sld.SingleLineDiagram;
import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.SimulationStatus;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.observation.ObservationField;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.TransportSolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class EssimLoadFlowController {
    private final static AtomicInteger threadCounter = new AtomicInteger(0);
    private final static ThreadFactory threadFactory = r -> new Thread(r, EssimLoadFlowController.class.getSimpleName() + "-" + threadCounter.incrementAndGet());

    /**
     * The translationMap maps ESDL assets ids to powsybl elements like generators and loads
     */
    private final Map<String, Object> translationMap = new HashMap<>();
    /**
     * The reverse map is used to lookup ESDL assets based on powsybl ids
     */
    private final Map<String, EnergyAsset> reverseTranslationMap = new HashMap<>();
    /**
     * The carrier map is used to lookup which carrier a powsybl component belongs to
     */
    private final Map<String, Carrier> carrierMap = new HashMap<>();
    /**
     * The carrier solver map is used to lookup which observer name we should use per carrier
     */
    private final Map<String, TransportSolver> carrierSolverMap = new HashMap<>();
    /**
     * The busIdMap maps ESDL port ids to the id of the powsybl bus that the port is/will be connected to.
     */
    private final Map<String, String> busIdMap = new HashMap<>();
    /**
     * The busMap is used to keep track of the busses that have been created
     */
    private final Map<String, Bus> busMap = new HashMap<>();
    /**
     * The voltageLevelMap is used to keep track of the created voltage levels
     */
    private final Map<String, VoltageLevel> voltageLevelMap = new HashMap<>();
    /**
     * The substationMap is used to map asset ids to the substations they belong to
     */
    private final Map<String, Substation> substationMap = new HashMap<>();

    /**
     * A mapping of unique asset identifiers (represented as strings) to their corresponding
     * {@link EnergyAsset} instances.
     */
    private Map<String, EnergyAsset> assetMap;

    private final ExecutorService executor;
    private Network network;
    private final String networkName;
    private final AtomicInteger simulationRunCounter = new AtomicInteger(1000);
    SimulationStatus status = SimulationStatus.UNDEFINED;


    public EssimLoadFlowController(String simulationId) {
        this.networkName = simulationId;
        executor = Executors.newSingleThreadExecutor(threadFactory);
    }

    public void createNetwork(List<TransportSolver> solvers, List<EnergyAsset> assetList) {
        try {
            Future<?> future = executor.submit(() -> {
                createNetworkInternal(solvers, assetList);
                return null;
            });
            future.get();
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createNetworkInternal(List<TransportSolver> solvers, List<EnergyAsset> assetList) throws IOException {
        /*
         * We may need a substation, because many elements can only be created in a substation
         * Substations contain voltage levels and transformers
         * VoltageLevels contain generators, loads and batteries. And buses and breakers (in busbreaker view getBusBreakerView().newBus())
         *                                                        OR nodes, but we don't use nodes
         * Network contains substations and lines and voltage levels. And also subnetworks....
         *
         * So we group devices based on connections. If devices are connected to each other without an electricity cable
         * between then, or they are connected to the same Bus or ElectricalConnection they will be grouped together in
         * the same substation and voltage level. A bus will be created in the voltage level and it will get an ID based
         * on this list. All the ports in the group will then be connected to this bus.
         *
         * Then we will create:
         * - A generator for a producer
         * - A generator (not a load) for a consumer (this is done to allow flexibility. When we choose a load, the power
         *      consumption cannot be changed. A generator with negative production can change the power consumption based
         *      on the rest of the network)
         * - A Battery for a storage
         * - A generator for a conversion
         * - A line for each line
         */
        log.info(MarkerFactory.getMarker(networkName), "Creating powsybl network {}", networkName);
        network = Network.create(networkName, "code");
        network.setMinimumAcceptableValidationLevel(ValidationLevel.STEADY_STATE_HYPOTHESIS);

        // Now we create the assets per carrier
        solvers.stream().filter(s -> s.getCarriers().iterator().next() instanceof ElectricityCommodity).forEach(solver -> {
            Set<EnergyAsset> connectedAssets = solver.getRoleMap().keySet();
            // Create the (producer/consumer/conversion) network elements for solvers
            createAssets(connectedAssets, solver.getCarriers());
        });
        // Create the transformers connecting the carriers
        createTransformers(assetList);
        // Now we create the transport lines
        createLines(assetList, network);
        // Fill the carrierSolverMap
        solvers.forEach(solver -> solver.getCarriers().forEach(c -> carrierSolverMap.put(c.getId(), solver)));
        // Fill the asset id map
        assetMap = assetList.stream().collect(Collectors.toMap(EnergyAsset::getId, a -> a));
        exportNetwork();
    }

    private void exportNetwork() throws IOException {
        Exporter ex = new XMLExporter();
        Path diagramsDir = Paths.get(System.getProperty("java.io.tmpdir"), "diagrams");
        if (!Files.exists(diagramsDir)) {
            Files.createDirectories(diagramsDir);
        }
        DataSource dataSource = Exporters.createDataSource(diagramsDir.resolve("essim-network-" + networkName + ".xml"));
        ex.export(network, new Properties(), dataSource);
        NetworkAreaDiagram.draw(network, diagramsDir.resolve("essim-network-" + networkName + "-diagram.svg"));
        List<String> list = substationMap.values().stream().map(Substation::getId).toList();
        list.forEach(i -> SingleLineDiagram.drawSubstation(network, i, diagramsDir.resolve("essim-network-" + networkName + "-substation-" + i + "-diagram.svg")));
//        SingleLineDiagram.drawMultiSubstations(network, voltageLevelMap.values().stream().map(v -> v.getSubstation().get().getId()).toList(), Paths.get("diagrams", "essim-network-" + networkName + "-single-line-diagram.svg"));
    }

    private void createTransformers(List<EnergyAsset> assetList) {
        assetList.stream().filter(a -> a instanceof Transformer).forEach(esdlTransformer -> {
            String tfTypeName = esdlTransformer.getAssetType();
            if (tfTypeName == null || tfTypeName.equals("")) {
                log.warn(MarkerFactory.getMarker(networkName), "No transformer type for transformer {} ({}) assuming default type '160 MVA 380/110 kV'", esdlTransformer.getName(), esdlTransformer.getId());
                tfTypeName = "160 MVA 380/110 kV";
            }
            TransformerSpec transformerSpec = TransformerSpec.getTransformerSpec(tfTypeName);
            if (transformerSpec == null) {
                log.warn(MarkerFactory.getMarker(networkName), "No transformer spec for transformer {} ({})", esdlTransformer.getName(), esdlTransformer.getId());
                throw new IllegalStateException("Invalid transformer type: " + tfTypeName + " for transformer " + esdlTransformer.getName() + " (" + esdlTransformer.getId() + ")");
            }
            // U1 connected to high voltage
            // U2 connected to low voltage
            if (esdlTransformer.getPort().size() != 2) {
                log.error(MarkerFactory.getMarker(networkName), "Transformer {} ({}) has {} ports, expected 2 ports", esdlTransformer.getName(), esdlTransformer.getId(), esdlTransformer.getPort().size());
                throw new IllegalStateException("Transformer can only have 2 ports");
            }
            Port portA = esdlTransformer.getPort().get(0);
            Port portB = esdlTransformer.getPort().get(1);
            Substation subStation = getSubStation(esdlTransformer);

            Port portLv = ((ElectricityCommodity) portA.getCarrier()).getVoltage() < ((ElectricityCommodity) portB.getCarrier()).getVoltage() ? portA : portB;
            Port portHv = portLv.equals(portA) ? portB : portA;
            VoltageLevel voltageLevelLv = getVoltageLevel(subStation, (ElectricityCommodity) portLv.getCarrier());
            VoltageLevel voltageLevelHv = getVoltageLevel(subStation, (ElectricityCommodity) portHv.getCarrier());
            Bus busLv = getBus((ElectricityCommodity) portLv.getCarrier(), voltageLevelLv, esdlTransformer);
            Bus busHv = getBus((ElectricityCommodity) portHv.getCarrier(), voltageLevelHv, esdlTransformer);

            double rk = transformerSpec.getVkrPercent() / 100.0;
            double zk = transformerSpec.getVkPercent() / 100.0;
            double xk = Math.sqrt(zk * zk - rk * rk);
            double ym = transformerSpec.getI0Percent() / 100.0;
            double gm = transformerSpec.getPfeKw() / (transformerSpec.getSnMva() * 1000.0);
            double bm = -1 * Math.signum(ym) * Math.sqrt(ym * ym - gm * gm);
            double zb_tr = transformerSpec.getVnLvKv() * transformerSpec.getVnLvKv() / transformerSpec.getSnMva();
            double r = rk * zb_tr;
            double x = xk * zb_tr;
            double g = gm / zb_tr;
            double b = bm / zb_tr;

            TwoWindingsTransformer transformer = subStation.newTwoWindingsTransformer()
                    .setRatedU2(transformerSpec.getVnHvKv())
                    .setRatedU1(transformerSpec.getVnLvKv())
                    .setR(r)
                    .setX(x)
                    .setB(b)
                    .setG(g)
                    .setBus2(busHv.getId())
                    .setConnectableBus2(busHv.getId())
                    .setBus1(busLv.getId())
                    .setConnectableBus1(busLv.getId())
                    .setVoltageLevel2(voltageLevelHv.getId())
                    .setVoltageLevel1(voltageLevelLv.getId())
                    .setId(esdlTransformer.getId() + "-transformer")
                    .add();
            reverseTranslationMap.put(transformer.getId(), esdlTransformer);
            translationMap.put(esdlTransformer.getId(), transformer);
            // A convention (for now): add the transformer to the low-side carrier
            carrierMap.put(transformer.getId(), portLv.getCarrier());
        });
    }

    private void createLines(List<EnergyAsset> connectedAssets, Network network) {
        connectedAssets.stream().filter(a -> a instanceof ElectricityCable).forEach(cable -> {
            if (cable.getPort().size() != 2) {
                log.error(MarkerFactory.getMarker(networkName), "Invalid number of ports for cable {} ({})", cable.getName(), cable.getId());
                throw new IllegalStateException("Electricity cable with " + cable.getPort().size() + " ports not allowed!");
            }
            Iterator<Port> iterator = cable.getPort().iterator();
            Port port1 = iterator.next();
            Port port2 = iterator.next();
            Carrier carrier = port1.getCarrier();
            Port connected1 = Commons.getConnectedPorts(port1).stream().filter(p -> !(p.getEnergyasset() instanceof ElectricityCable)).findAny().get();
            Port connected2 = Commons.getConnectedPorts(port2).stream().filter(p -> !(p.getEnergyasset() instanceof ElectricityCable)).findAny().get();
            VoltageLevel voltageLevel1 = getVoltageLevel(getSubStation(connected1.getEnergyasset()), (ElectricityCommodity) connected1.getCarrier());
            VoltageLevel voltageLevel2 = getVoltageLevel(getSubStation(connected2.getEnergyasset()), (ElectricityCommodity) connected2.getCarrier());
            Bus bus1 = getBus((ElectricityCommodity) carrier, voltageLevel1, cable);
            Bus bus2 = getBus((ElectricityCommodity) carrier, voltageLevel2, cable);
            int parallelLines = Math.max(cable.getAggregationCount(), 1);
            String cableTypeName = cable.getAssetType();
            CableSpec cableSpec = CableSpec.getCableSpec(cableTypeName);
            if (cableSpec == null) {
                cableTypeName = "243-AL1/39-ST1A 20.0";
                log.warn(MarkerFactory.getMarker(networkName), "No asset type for cable {} ({}) assuming default type '{}'", cable.getName(), cable.getId(), cableTypeName);
                cableSpec = CableSpec.getCableSpec(cableTypeName);
            }
            log.debug(MarkerFactory.getMarker(networkName), "Creating cable of type {}", cableTypeName);
            double x = ((ElectricityCable) cable).getLength() * cableSpec.getXOhmPerKm() / (1000.0 * parallelLines);
            double r = ((ElectricityCable) cable).getLength() * cableSpec.getROhmPerKm() / (1000.0 * parallelLines);
            // TODO assuming 50Hz frequency here, get the actual frequency from somewhere
            double b = (((ElectricityCable) cable).getLength() * cableSpec.getCNfPerKm() * parallelLines * 1e-9 * 2.0 * Math.PI * 50.0) / 2.0;
//            double g = ((ElectricityCable) cable).getLength() * cableSpec.get
            Line line = network.newLine()
                    .setBus1(bus1.getId())
                    .setBus2(bus2.getId())
                    .setConnectableBus1(bus1.getId())
                    .setConnectableBus2(bus2.getId())
                    .setVoltageLevel1(voltageLevel1.getId())
                    .setVoltageLevel2(voltageLevel2.getId())
                    .setX(x)
                    .setR(r)
                    .setB1(b)
                    .setB2(b)
                    .setId("electricityCable-" + cable.getId())
                    .add();
            line.newOperationalLimitsGroup1("GLOBAL")
                    .newCurrentLimits()
                    .setPermanentLimit(cableSpec.getMaxIkA() * 1000.0 * parallelLines)
                    .add();
            line.setSelectedOperationalLimitsGroup1("GLOBAL");
            translationMap.put(cable.getId(), line);
            reverseTranslationMap.put(line.getId(), cable);
            carrierMap.put(line.getId(), carrier);
        });
    }

    List<Port> getSortedBusPortList(Port port) {
        Set<Port> portSet = new HashSet<>();
        portSet.add(port);
        Commons.addConnectedPortsToSet(portSet, EssimLoadFlowController::isSubstationInternalConnection);
        return portSet.stream().sorted(Comparator.comparing(Port::getId)).toList();
    }

    public static boolean isSubstationInternalConnection(EnergyAsset asset) {
        return asset instanceof EConnection || asset instanceof esdl.Bus;
    }

    /**
     * Retrieves the first port associated with a given energy asset and electricity commodity carrier.
     *
     * @param asset   the energy asset containing ports to be evaluated
     * @param carrier the electricity commodity carrier to be matched with ports
     * @return an Optional containing the first port matching the specified carrier, or an empty Optional if no match is found
     */
    Optional<Port> getFirstPortForCarrier(EnergyAsset asset, ElectricityCommodity carrier) {
        return asset.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).min(Comparator.comparing(Port::getId));
    }

    /**
     * Retrieves the ID of the bus the given port is connected to.
     *
     * @param port the port for which the connected bus ID is needed
     * @return the connected bus ID as a string
     */
    private String getConnectedBusId(Port port) {
        List<Port> substationPortList = getSortedBusPortList(port);
        String id = substationPortList.getFirst().getId();
        return busIdMap.get(id);
    }

    /**
     * Retrieves the carrier associated with a port of the given energy asset that matches one of the carriers in the specified set.
     *
     * @param energyAsset the energy asset containing ports to be evaluated
     * @param carriers    the set of carriers to match with the ports of the energy asset
     * @return the carrier associated with a matching port, or null if no match is found
     */
    private Carrier getConnectedCarrier(EnergyAsset energyAsset, Set<Carrier> carriers) {
        for (Port port : energyAsset.getPort()) {
            if (carriers.contains(port.getCarrier())) {
                return port.getCarrier();
            }
        }
        return null;
    }


    private void createAssets(Collection<EnergyAsset> assetList, Set<Carrier> carriers) {
        AtomicInteger producerCounter = new AtomicInteger();
        AtomicInteger consumerCounter = new AtomicInteger();
        AtomicInteger storageCounter = new AtomicInteger();
        AtomicInteger conversionCounter = new AtomicInteger();
        // We will first create a bus for each bus in the ESDL file. More buses will probably be created based on the
        // connections in the ESDL file
        assetList.stream().filter(a -> a instanceof esdl.Bus).forEach(busAsset -> {
            ElectricityCommodity carrier = (ElectricityCommodity) getConnectedCarrier(busAsset, carriers);
            VoltageLevel voltageLevel = getVoltageLevel(getSubStation(busAsset), carrier);
            Bus bus = getBus(carrier, voltageLevel, busAsset);
            carrierMap.put(bus.getId(), carrier);

        });
        assetList.stream().filter(a -> a instanceof Producer).forEach(producer -> {
            ElectricityCommodity carrier = (ElectricityCommodity) getConnectedCarrier(producer, carriers);
            VoltageLevel voltageLevel = getVoltageLevel(getSubStation(producer), carrier);
            Bus bus = getBus(carrier, voltageLevel, producer);
            Generator generator = createProducer(voltageLevel, bus.getId(), (Producer) producer);
            carrierMap.put(generator.getId(), carrier);
            producerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Consumer).forEach(consumer -> {
            ElectricityCommodity carrier = (ElectricityCommodity) getConnectedCarrier(consumer, carriers);
            VoltageLevel voltageLevel = getVoltageLevel(getSubStation(consumer), carrier);
            Bus bus = getBus(carrier, voltageLevel, consumer);
            Generator load = createConsumer(voltageLevel, bus.getId(), consumer);
            carrierMap.put(load.getId(), carrier);
            consumerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Storage).forEach(storage -> {
            ElectricityCommodity carrier = (ElectricityCommodity) getConnectedCarrier(storage, carriers);
            VoltageLevel voltageLevel = getVoltageLevel(getSubStation(storage), carrier);
            Bus bus = getBus(carrier, voltageLevel, storage);
            Battery battery = createStorage(voltageLevel, bus.getId(), (Storage) storage);
            carrierMap.put(battery.getId(), carrier);
            storageCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof AbstractBasicConversion).forEach(conversion -> {
            ElectricityCommodity carrier = (ElectricityCommodity) getConnectedCarrier(conversion, carriers);
            VoltageLevel voltageLevel = getVoltageLevel(getSubStation(conversion), carrier);
            Bus bus = getBus(carrier, voltageLevel, conversion);
            createConversion(voltageLevel, carrier, bus.getId(), (AbstractBasicConversion) conversion);
            conversionCounter.incrementAndGet();
        });
        log.debug(MarkerFactory.getMarker(networkName), "Created {} producers, {} consumers, {} storages and {} conversions", producerCounter.get(), consumerCounter.get(), storageCounter.get(), conversionCounter.get());
    }


    private Substation getSubStation(EnergyAsset asset) {
        if (asset instanceof ElectricityCable) {
            throw new IllegalArgumentException("An ElectricityCable is not located in a substation");
        }
        Substation substation = substationMap.get(asset.getId());
        if (substation == null) {
            // We get a sorted list of all the assets that belong to this substation.
            List<EnergyAsset> list = getSubstationAssets(asset.getPort());
            // The id of the substation is a concatenation of all the names or (part of) the ids of the contained assets
            String id = "substation-" + list.stream().map(a -> a.getName() == null || "".equals(a.getName()) ? a.getId().substring(0, 4) : a.getName()).collect(Collectors.joining("-"));
            Substation newSubstation = network.newSubstation().setId(id).setCountry(Country.NL).add();
            // Add all the assets to the map, mapping to this new substation
            substationMap.putAll(list.stream().collect(Collectors.toMap(EnergyAsset::getId, a -> newSubstation)));
            log.trace(MarkerFactory.getMarker(networkName), "Creating substation {} for assets: {}", id, list.stream().map(EnergyAsset::getName).collect(Collectors.joining(", ")));
            substation = newSubstation;
        }
        return substation;
    }

    /**
     * Get a list of energy assets that should be together in a substation. This is done by recursively finding all the
     * assets that are connected directly to the given list of ports. 'Connected directly' in this case means they are
     * connected, but not through electricity cables (asset type {@code ElectricityCable}).
     *
     * @param ports the list of ports to analyze and determine connected energy assets.
     * @return a list of distinct {@code EnergyAsset} objects, somehow connected to the given ports.
     */
    private List<EnergyAsset> getSubstationAssets(List<Port> ports) {
        HashSet<Port> portSet = new HashSet<>();
        addSubstationInternalPorts(ports, portSet);
        return portSet.stream().map(Port::getEnergyasset).distinct().filter(a -> !(a instanceof ElectricityCable)).sorted(Comparator.comparing(EnergyAsset::getId)).toList();
    }

    private void addSubstationInternalPorts(List<Port> ports, HashSet<Port> portSet) {
        portSet.addAll(ports);
        Stream<Port> portStream = ports.stream().filter(p -> p.getCarrier() instanceof ElectricityCommodity).flatMap(p -> Commons.getConnectedPorts(p).stream());
        portStream.forEach(p -> {
            if (!portSet.contains(p)) {
                EnergyAsset energyAsset = p.getEnergyasset();
                if (energyAsset instanceof ElectricityCable) {
                    addSubstationInternalPorts(List.of(p), portSet);
                } else {
                    addSubstationInternalPorts(energyAsset.getPort().stream().filter(p2 -> p2.getCarrier() instanceof ElectricityCommodity).collect(Collectors.toList()), portSet);
                }
            }
        });

    }

    /**
     * Retrieves or creates a Bus object to which the port of a specific energy asset, connected to the given
     * electricity carrier, should be connected. If no bus exists for the given asset and carrier, a new bus is
     * created and configured using the relevant substation port and voltage level information.
     *
     * @param carrier the electricity commodity used to identify the carrier associated with the energy asset
     * @param asset   the energy asset whose associated bus is being retrieved or created
     * @return the Bus object to which the port of the specified asset, connected to the given carrier, should connect
     * @throws IllegalStateException if the specified energy asset is not connected to the given carrier
     */
    private Bus getBus(ElectricityCommodity carrier, VoltageLevel voltageLevel, EnergyAsset asset) {
        Optional<Port> firstPortForCarrier = getFirstPortForCarrier(asset, carrier);
        if (firstPortForCarrier.isEmpty()) {
            log.error(MarkerFactory.getMarker(networkName), "Asset {} is not connected to carrier {}}", asset.getId(), carrier.getId());
            throw new IllegalStateException(String.format("Asset %s is not connected to carrier %s", asset.getName(), carrier.getName()));
        }
        String ssString = voltageLevel.getSubstation().isEmpty() ? "network-" : voltageLevel.getSubstation().get().getId() + "-";
        String busId = "bus-" + ssString + voltageLevel.getId();
        Bus bus = busMap.get(busId);
        if (bus == null) {
            String actualId = busId;
            String actualName = busId;
            if (asset instanceof esdl.Bus busAsset) {
                actualId = busAsset.getId();
                actualName = busAsset.getName() == "" ? busAsset.getId() : busAsset.getName();
            }
            bus = voltageLevel.getBusBreakerView().newBus().setId(actualId).setName(actualName).add();
            log.info(MarkerFactory.getMarker(networkName), "Creating bus with id {} in voltagelevel {}", actualName, voltageLevel.getId());
            busMap.put(busId, bus);
        }
        return bus;
    }

    private VoltageLevel getVoltageLevel(Substation substation, ElectricityCommodity carrier) {
        String carrierName = carrier.getName() == null ? carrier.getId() : carrier.getName();
        String voltageLevelId = "voltageLevel-" + carrierName + "-" + substation.getId();
        VoltageLevel voltageLevel = voltageLevelMap.get(voltageLevelId);
        if (voltageLevel == null) {
            voltageLevel = substation.newVoltageLevel()
                    .setId(voltageLevelId)
                    .setNominalV((carrier).getVoltage() / 1000.0)
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .add();
            voltageLevelMap.put(voltageLevelId, voltageLevel);
            log.trace(MarkerFactory.getMarker(networkName), "Creating VoltageLevel {} for substation {} and carrier {}", voltageLevelId, substation.getId(), carrierName);
        }
        return voltageLevel;
    }

    private Object createConversion(VoltageLevel voltageLevel, ElectricityCommodity carrier, String busId, AbstractBasicConversion conversion) {
        AtomicReference<Object> result = new AtomicReference<>();
        conversion.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).findFirst().ifPresent(port -> {
            GeneratorAdder generatorAdder = voltageLevel.newGenerator()
                    .setId("conversion-" + conversion.getId())
                    .setName(conversion.getName())
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setBus(busId)
                    .setConnectableBus(busId)
                    .setVoltageRegulatorOn(true)
                    .setTargetV(voltageLevel.getNominalV());
            // TODO do we need to convert based on port?
            if (port instanceof InPort) {
                generatorAdder.setMinP(-conversion.getPower() / 1000.0);
            }
            else if (port instanceof OutPort) {
                generatorAdder.setMaxP(conversion.getPower() / 1000.0);
            }
            Generator generator = generatorAdder.add();
            generator.newMinMaxReactiveLimits()
                    .setMinQ(-9999.99)
                    .setMaxQ(9999.99);
            log.trace(MarkerFactory.getMarker(networkName), "Conversion asset {} is implemented as a generator: {}", conversion.getClass().getInterfaces()[0].getSimpleName(), generator);
            translationMap.put(conversion.getId(), generator);
            reverseTranslationMap.put(generator.getId(), conversion);

            result.set(conversion);
            translationMap.put(conversion.getId(), conversion);
            reverseTranslationMap.put(conversion.getId(), conversion);
            carrierMap.put(conversion.getId(), carrier);
        });
        return result.get();
    }

    private Battery createStorage(VoltageLevel voltageLevel, String busId, Storage storage) {
        if (storage.getPort().size() > 1) {
            log.error(MarkerFactory.getMarker(networkName), "Batteries with multiple ports not supported in loadflow for battery {} ({})", storage.getName(), storage.getId());
            throw new IllegalStateException("Batteries with multiple ports not supported in loadflow");
        }
        Battery battery = voltageLevel.newBattery()
                .setBus(busId)
                .setConnectableBus(busId)
                .setId("storage-" + storage.getId())
                .setName(storage.getName())
                .setMaxP(storage.getMaxChargeRate() / 1000.0)
                .setMinP(-storage.getMaxDischargeRate() / 1000.0)
                .setTargetP(0)
                .setTargetQ(0)
                .add();
        translationMap.put(storage.getId(), battery);
        reverseTranslationMap.put(battery.getId(), storage);

        log.trace(MarkerFactory.getMarker(networkName), "Asset {} is implemented as a storage: {}", storage.getClass().getInterfaces()[0].getSimpleName(), battery.toString());
        return battery;
    }

    private Generator createConsumer(VoltageLevel voltageLevel, String busId, EnergyAsset consumer) {
        if (consumer.getPort().size() > 1) {
            log.error(MarkerFactory.getMarker(networkName), "with multiple ports not supported in loadflow for battery {} ({})", consumer.getName(), consumer.getId());
            throw new IllegalStateException("Consumers with multiple ports are not supported in loadflow");
        }
        Generator generator = voltageLevel.newGenerator()
                .setId("consumer-" + consumer.getId())
                .setName(consumer.getName())
                .setMinP(0)
                .setMaxP(Double.MAX_VALUE)
                .setTargetP(0)
                .setTargetQ(0)
                .setBus(busId)
                .setConnectableBus(busId)
                .setVoltageRegulatorOn(true)
                .setTargetV(voltageLevel.getNominalV())
                .add();
        generator.newMinMaxReactiveLimits()
                .setMinQ(-9999.99)
                .setMaxQ(9999.99);
        log.trace(MarkerFactory.getMarker(networkName), "Consumer asset {} is implemented as a generator: {}", consumer.getClass().getInterfaces()[0].getSimpleName(), generator.toString());
        translationMap.put(consumer.getId(), generator);
        reverseTranslationMap.put(generator.getId(), consumer);
        return generator;
    }

    private Generator createProducer(VoltageLevel voltageLevel, String busId, Producer producer) {
        if (producer.getPort().size() > 1) {
            log.error(MarkerFactory.getMarker(networkName), "Producer with multiple ports not supported in loadflow for battery {} ({})", producer.getName(), producer.getId());
            throw new IllegalStateException("Producers with multiple are ports not supported in loadflow");
        }

        Generator generator = voltageLevel.newGenerator()
                .setId("producer-" + producer.getId())
                .setName(producer.getName())
                .setMinP(0)
                .setMaxP(producer.getPower() / 1000.0)
                .setTargetP(0)
                .setTargetQ(0)
                .setBus(busId)
                .setConnectableBus(busId)
                .setVoltageRegulatorOn(true)
                .setTargetV(voltageLevel.getNominalV())
                .add();
        generator.newMinMaxReactiveLimits()
                .setMinQ(-9999.99)
                .setMaxQ(9999.99);
        log.trace(MarkerFactory.getMarker(networkName), "Producer asset {} is implemented as a generator: {}", producer.getClass().getInterfaces()[0].getSimpleName(), generator.toString());
        translationMap.put(producer.getId(), generator);
        reverseTranslationMap.put(generator.getId(), producer);
        return generator;
    }

    private void assignPowerValues(IObservation observation) {
        String assetId = observation.getTags().get("assetId");
        String assetName = observation.getTags().get("assetName");
        Double powerVal = (Double) observation.getValues().get("allocationPower");
        Object powsyblObject = translationMap.get(assetId);
        if (powerVal == null || powsyblObject == null) {
            return;
        }
        // Powsybl default unit is kW, so we divide by 1000
        powerVal = powerVal / 1000.0;
        log.trace(MarkerFactory.getMarker(networkName), "Power allocation for {} is {} kW", assetName, powerVal);
        EnergyAsset energyAsset = assetMap.get(assetId);
        Double maxPower = null;
        Double minPower = null;
        ControlStrategy controlStrategy = energyAsset.getControlStrategy();
        if (controlStrategy instanceof DrivenByProfile) {
            maxPower = powerVal;
            minPower = powerVal;
        }
        if (controlStrategy instanceof CurtailmentStrategy) {
            maxPower = ((CurtailmentStrategy) controlStrategy).getMaxPower() / 1000.0;
        }

        if (minPower != null || maxPower != null) {
            log.trace(MarkerFactory.getMarker(networkName), "Power boundaries for {} are {} kW and {} kW", assetName, minPower, maxPower);
        }
        if (powsyblObject instanceof Load load) {
            load.setP0(Math.abs(powerVal)).setQ0(0);
        } else if (powsyblObject instanceof Generator generator) {
            generator.setTargetP(-powerVal);
            if (maxPower != null) {
                generator.setMaxP(-maxPower);
            }
            if (minPower != null) {
                generator.setMinP(-minPower);
            }
        } else if (powsyblObject instanceof Battery battery) {
            if (maxPower != null) {
                battery.setMaxP(maxPower);
            }
            if (minPower != null) {
                battery.setMinP(minPower);
            }
            battery.setTargetP(Math.abs(powerVal));
            battery.setTargetP(-powerVal);
        }
    }

    private void run(EssimTime timestamp) {
        LocalDateTime time = timestamp.getTime();
        ZoneId zoneId = ZoneId.systemDefault();
        network.setCaseDate(time.atZone(zoneId));
        LoadFlowParameters parameters = LoadFlowParameters.load()
                .setUseReactiveLimits(false)
                .setDistributedSlack(true);
        parameters.getExtension(OpenLoadFlowParameters.class)
                .setPlausibleActivePowerLimit(Double.MAX_VALUE)
//                .setUseActiveLimits(false)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED)
//                .setSlackBusSelectionMode(SlackBusSelectionMode.LARGEST_GENERATOR)
                .setSlackDistributionFailureBehavior(OpenLoadFlowParameters.SlackDistributionFailureBehavior.LEAVE_ON_SLACK_BUS)
//                .setSlackDistributionFailureBehavior(OpenLoadFlowParameters.SlackDistributionFailureBehavior.DISTRIBUTE_ON_REFERENCE_GENERATOR)
//                .setAcSolverType(FastDecoupledFactory.NAME)
//                .setAcSolverType(NewtonKrylovFactory.NAME)
                .setAcSolverType(NewtonRaphsonFactory.NAME)
                .setMaxNewtonRaphsonIterations(100)
        // The next two settings are for "Fast decoupled" AC solver
//                .setLineSearchStateVectorScalingMaxIteration(4)
//                .setLineSearchStateVectorScalingStepFold(3.0/2.0)
        ;

        OpenLoadFlowProvider loadFlowProvider = new OpenLoadFlowProvider();
        LoadFlow.Runner loadFlowRunner = new LoadFlow.Runner(loadFlowProvider);

        LoadFlowResult result = loadFlowRunner.run(network, parameters);
        result.getComponentResults().forEach(cr -> {
            if (cr.getStatus() != LoadFlowResult.ComponentResult.Status.CONVERGED && cr.getStatus() != LoadFlowResult.ComponentResult.Status.NO_CALCULATION) {
                log.warn("At timestep {}: Component {} state is {} ({})", time, cr.getReferenceBusId(), cr.getStatus(), cr.getStatusText());
            }
        });
    }

    @NotNull
    private String updateVariantId() {
        final String variantId = "loadflowVariant-" + simulationRunCounter.incrementAndGet();
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
        network.getVariantManager().setWorkingVariant(variantId);
        return variantId;
    }

    private Observation.ObservationBuilder createObservationBuilder(EssimTime timestamp, EnergyAsset asset, Carrier carrier) {
        Observation.ObservationBuilder observation = Observation.builder()
                .observedAt(timestamp.getTime())
                .tag("assetId", asset.getId())
                .tag("assetName", asset.getName() == null ? "UnnamedAsset" : asset.getName())
                .tag("assetClass", asset.getClass().getInterfaces()[0].getSimpleName())
                .tag("carrierId", carrier.getId())
                .tag("carrierName", carrier.getName() == null ? "UnnamedCarrier" : carrier.getName());
        return observation;
    }

    public ArrayList<Observation> step(EssimTime timestamp, ArrayList<Observation> observations) {
        Future<ArrayList<Observation>> future = executor.submit(() -> processObservations(timestamp, observations));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ArrayList<Observation>
    processObservations(EssimTime timestamp, ArrayList<Observation> observations) {
        // Update the variant ID for the simulation
        updateVariantId();
        observations.forEach(this::assignPowerValues);
        status = SimulationStatus.STARTED;
        run(timestamp);
        HashMap<String, Observation> observationMap = new HashMap<>();

        final TopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                Observation observation = createObservation(generator.getId(), generator.getTerminal(), timestamp);
                log.trace(MarkerFactory.getMarker(networkName), "Adding observation for generator {}: {}", generator.getId(), observation.toString());
                observationMap.put(getObservationKey(observation), observation);
            }

            @Override
            public void visitLoad(Load load) {
                Observation observation = createObservation(load.getId(), load.getTerminal(), timestamp);
                log.trace(MarkerFactory.getMarker(networkName), "Adding observation for load {}: {}", load.getId(), observation.toString());
                observationMap.put(getObservationKey(observation), observation);
            }

            @Override
            public void visitBattery(Battery battery) {
                Observation observation = createObservation(battery.getId(), battery.getTerminal(), timestamp);
                log.trace(MarkerFactory.getMarker(networkName), "Adding observation for battery {}: {}", battery.getId(), observation.toString());
                observationMap.put(getObservationKey(observation), observation);
            }

            @Override
            public void visitLine(Line line, TwoSides side) {
                String id = line.getId();
                Carrier carrier = getCarrier(id);
                Observation observation = createBranchObservation(line, side, carrier, timestamp);
                log.trace(MarkerFactory.getMarker(networkName), "Adding observation for line {}: {}", id, observation.toString());
                observationMap.put(getObservationKey(observation), observation);
                super.visitLine(line, side);
            }

            @Override
            public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, TwoSides side) {
                Carrier carrier = getCarrier(transformer.getId());
                Observation observation = createBranchObservation(transformer, side, carrier, timestamp);
                observationMap.put(getObservationKey(observation), observation);
                log.trace(MarkerFactory.getMarker(networkName), "Adding observation for transformer {}: {}", transformer.getId(), observation.toString());
                super.visitTwoWindingsTransformer(transformer, side);
            }
        };
        voltageLevelMap.values().forEach(voltageLevel -> voltageLevel.visitEquipments(visitor));

        for (var i = 0; i < observations.size(); i++) {
            Observation observation = observations.get(i);
            Observation.ObservationBuilder builder = observation.toBuilder();
            Observation lfObservation = observationMap.get(getObservationKey(observation));
            if (lfObservation != null) {
                lfObservation.getValues().forEach((key, value) -> builder.value(ObservationField.valueOfKey(key), value));
            }
            observations.set(i, builder.build());
        }
        return observations;
    }

    private Carrier getCarrier(String id) {
        return carrierMap.get(id);
    }

    private Observation createBranchObservation(Branch branch, TwoSides side, Carrier carrier, EssimTime timestamp) {
        Observation.ObservationBuilder builder = createObservationBuilder(timestamp, reverseTranslationMap.get(branch.getId()), carrier);
        double p1 = Math.abs(branch.getTerminal(TwoSides.TWO).getP());
        double p2 = Math.abs(branch.getTerminal(TwoSides.ONE).getP());
        double p = p1 > p2 ? p1 - p2 : p2 - p1;
        double q1 = Math.abs(branch.getTerminal(TwoSides.TWO).getQ());
        double q2 = Math.abs(branch.getTerminal(TwoSides.ONE).getQ());
        double q = q1 > q2 ? q1 - q2 : q2 - q1;
        builder.value(ObservationField.LOADFLOW_POWER, p * 1000.0)
                .value(ObservationField.LOADFLOW_ENERGY, p * 1000.0 * timestamp.getSimulationStepLength().getSeconds())
                .value(ObservationField.LOADFLOW_REACTIVE_POWER, q * 1000.0);
        if (branch instanceof Line) {
            builder.value(ObservationField.LOADFLOW_CURRENT, branch.getTerminal(side).getI());

        }
        return builder.build();
    }

    private static String getObservationKey(Observation observation) {
        return observation.getTags().get("assetId") + "-" + observation.getTags().get("carrierId");
    }

    private Observation createObservation(String id, Terminal terminal, EssimTime timestamp) {
        Carrier carrier = getCarrier(id);
        EnergyAsset asset = reverseTranslationMap.get(id);
        String assetName = asset.getName() == null ? "" : asset.getName();
        assetName += "(" + asset.getId() + ")";
        Observation.ObservationBuilder builder = createObservationBuilder(timestamp, asset, carrier);
        double loadflowPower = terminal.getP() * 1000.0;
        double loadflowReactivePower = terminal.getQ() * 1000.0;
        if (Double.isNaN(loadflowPower) || Double.isNaN(loadflowReactivePower)) {
            log.debug(MarkerFactory.getMarker(networkName), "Loadflow power is {} , reactive power is {} for asset {} with terminal {}", loadflowPower, loadflowReactivePower, assetName, terminal);
        }
        Observation observation = builder.value(ObservationField.LOADFLOW_POWER, loadflowPower)
                .value(ObservationField.LOADFLOW_ENERGY, terminal.getP() * 1000.0 * timestamp.getSimulationStepLength().getSeconds())
                .value(ObservationField.LOADFLOW_REACTIVE_POWER, loadflowReactivePower)
                .build();
        return observation;
    }
}
