package nl.tno.essim.loadflow;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.serde.XMLExporter;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.sld.SingleLineDiagram;
import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.SimulationStatus;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.Observation;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.TransportSolver;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class EssimLoadFlowController {
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
     * The carrier name map is used to lookup which observer name we should use per carrier
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
    private Network network;
    private final String networkName;
    private final AtomicInteger simulationRunCounter = new AtomicInteger();
    SimulationStatus status = SimulationStatus.UNDEFINED;

    public EssimLoadFlowController(String networkName) {
        this.networkName = networkName;
    }

    public void createNetwork(List<TransportSolver> solvers, List<EnergyAsset> assetList) {
        /*
         * We may need a substation, because many elements can only be created in a substation
         * Substations contain voltagelevels and transformers
         * Voltagelevels contain generators, loads and batteries. And buses and breakers (in busbreaker view getBusBreakerView().newBus())
         *                                                        OR nodes, but I don't understand nodes yet
         * Network contains substations and lines and voltage levels. And also subnetworks....
         *
         * So we group devices based on connections. If devices are connected to each other without an electricity cable
         * between then, or they are connected to the same Bus or ElectricalConnection they will be grouped together in
         * the same substation and voltage level. Grouping is done in getSortedSubStationPortList(). A bus will be created
         * in the voltage level and it will get an ID based on this list. All the ports in the group will then be connected
         * to this bus.
         *
         * Then we will create:
         * - A generator for a produce
         * - A load for a consumer
         * - A Battery for a storage
         * - A generator OR a load for a conversion, based of the type of conversion
         * - A line for each line
         */
        log.info("Creating powsybl network {}", networkName);
        network = Network.create(networkName, "code");
        network.setMinimumAcceptableValidationLevel(ValidationLevel.STEADY_STATE_HYPOTHESIS);

        // Now we create the network per carrier
        solvers.forEach(solver -> {
            List<EnergyAsset> connectedAssets = assetList.stream().filter(a -> !a.getPort().stream().filter(p -> p.getCarrier() != null && p.getCarrier().equals(solver.getCarrier())).toList().isEmpty()).toList();
            createNetworkPerCarrier(solver.getCarrier(), connectedAssets);
        });
        solvers.forEach(solver -> carrierSolverMap.put(solver.getCarrier().getId(), solver));

        // Then we create the transformers connecting the carriers
        createTransformers(assetList);
    }

    private void exportNetwork() {
        Exporter ex = new XMLExporter();
        DataSource dataSource = Exporters.createDataSource(Paths.get("diagrams", "essim-network-" + networkName + ".xml"));
        ex.export(network, new Properties(), dataSource);
        NetworkAreaDiagram.draw(network, Paths.get("diagrams", "essim-network-" + networkName + "-diagram.svg"));
        SingleLineDiagram.drawMultiSubstations(network, voltageLevelMap.values().stream().map(v -> v.getSubstation().get().getId()).toList(), Paths.get("diagrams", "essim-network-" + networkName + "-single-line-diagram.svg"));
    }

    private void createTransformers(List<EnergyAsset> assetList) {
        assetList.stream().filter(a -> a instanceof Transformer).forEach(esdlTransformer -> {
            TransformerSpec transformerSpec = TransformerSpec.getTransformerSpec(esdlTransformer.getAssetType());
            if (transformerSpec == null) {
                throw new IllegalStateException("Cannot use a transformer without a type");
            }
            // For now: some random values for R an X
            // r="0.2665846" x="11.104493"
            // U1 connected to low voltage
            // U2 connected to high voltage
            if (esdlTransformer.getPort().size() != 2) {
                throw new IllegalStateException("Transformer can only have 2 ports");
            }
            Port portA = esdlTransformer.getPort().get(0);
            Port portB = esdlTransformer.getPort().get(1);

            Port port1 = ((ElectricityCommodity) portA.getCarrier()).getVoltage() < ((ElectricityCommodity) portB.getCarrier()).getVoltage() ? portA : portB;
            Port port2 = ((ElectricityCommodity) portA.getCarrier()).getVoltage() < ((ElectricityCommodity) portB.getCarrier()).getVoltage() ? portB : portA;
            // Let's choose the highest voltage for the substation
            VoltageLevel voltageLevel = getVoltageLevel(port2);
            TwoWindingsTransformer transformer = voltageLevel.getSubstation().get().newTwoWindingsTransformer()
                    .setRatedU1(transformerSpec.getVnLvKv())
                    .setRatedU2(transformerSpec.getVnHvKv())
                    .setRatedS(transformerSpec.getSnMva())
                    .setR(0.2665846)
                    .setX(11.104493)
                    .setB(0)
                    .setG(0)
                    .setBus1(getBusId(port1.getId()))
                    .setConnectableBus1(getBusId(port1.getId()))
                    .setBus2(getBusId(port2.getId()))
                    .setConnectableBus2(getBusId(port2.getId()))
                    .setVoltageLevel1(voltageLevelMap.get(port1.getCarrier()).getId())
                    .setVoltageLevel2(voltageLevelMap.get(port2.getCarrier()).getId())
                    .setId(esdlTransformer.getId() + "-transformer")
                    .add();
        });
    }

    private void createNetworkPerCarrier(Carrier carrier, List<EnergyAsset> connectedAssets) {
        // Create the network elements for the connected assets
        createAssets(connectedAssets, (ElectricityCommodity) carrier);

        // Create the cables
        createLines(connectedAssets, network, carrier);
    }

    private void createLines(List<EnergyAsset> connectedAssets, Network network, Carrier carrier) {
        connectedAssets.stream().filter(a -> a instanceof ElectricityCable).forEach(cable -> {
            if (cable.getPort().size() != 2) {
                throw new IllegalStateException("Electricity cable with " + cable.getPort().size() + " ports not allowed!");
            }
            ((ElectricityCable) cable).getLength();
            Iterator<Port> iterator = cable.getPort().iterator();
            Port port1 = iterator.next();
            Port port2 = iterator.next();
            String busId1 = getConnectedBusId(port1);
            String busId2 = getConnectedBusId(port2);
            String cableTypeName = cable.getAssetType();
            if (cableTypeName == null || cableTypeName.isEmpty()) {
                cableTypeName = "243-AL1/39-ST1A 20.0";
                log.warn("No asset type for cable {} ({}) assuming default type '{}'", cable.getName(), cable.getId(), cableTypeName);
            }
            CableSpec cableSpec = CableSpec.getCableSpec(cableTypeName);
            log.debug("Creating cable of type {}", cableTypeName);
            double x = ((ElectricityCable) cable).getLength() * cableSpec.getXOhmPerKm() / 1000.0;
            double r = ((ElectricityCable) cable).getLength() * cableSpec.getROhmPerKm() / 1000.0;
            Line line = network.newLine()
                    .setBus1(busId1)
                    .setBus2(busId2)
                    .setConnectableBus1(busId1)
                    .setConnectableBus2(busId2)
                    .setVoltageLevel1(getVoltageLevel(port1).getId())
                    .setVoltageLevel2(getVoltageLevel(port2).getId())
                    .setX(x)
                    .setR(r)
                    .setId("electricityCable-" + cable.getId())
                    .add();
            translationMap.put(cable.getId(), line);
            reverseTranslationMap.put(line.getId(), cable);
            carrierMap.put(line.getId(), carrier);
        });

    }

    void addConnectedPortsToSet(Set<Port> portSet) {
        Set<Port> connected = portSet.stream().flatMap(p -> getConnectedPorts(p).stream()).collect(Collectors.toSet());
        connected.addAll(portSet.stream().filter(p -> isSubstationInternalConnection(p.getEnergyasset())).flatMap(p -> p.getEnergyasset().getPort().stream()).toList());
        int prevSize = portSet.size();
        portSet.addAll(connected);
        if (portSet.size() > prevSize) {
            addConnectedPortsToSet(portSet);
        }
    }

    List<Port> getSortedSubstationPortList(Port port) {
        Set<Port> portSet = new HashSet<>();
        portSet.add(port);
        addConnectedPortsToSet(portSet);
        return portSet.stream().sorted(Comparator.comparing(Port::getId)).toList();
    }

    boolean isSubstationInternalConnection(EnergyAsset asset) {
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
     * Retrieves the list of ports that are connected to the specified port. Depending on the type of the input port
     * (either InPort or OutPort), it gathers the connected OutPorts or InPorts, respectively.
     *
     * @param port the port for which the connected ports are to be retrieved. This port should be an instance of either
     *             InPort or OutPort.
     * @return a list containing the ports that are directly connected to the specified port. If the input is of type
     * InPort, the result will include connected OutPorts; if it is of type OutPort, the result will include
     * connected InPorts.
     * @throws IllegalStateException if the specified port is not of type InPort or OutPort.
     */
    EList<Port> getConnectedPorts(Port port) {
        EList<Port> ports = new BasicEList<>();
        if (port instanceof InPort) {
            EList<OutPort> connectedTo = ((InPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else if (port instanceof OutPort) {
            EList<InPort> connectedTo = ((OutPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else {
            throw new IllegalStateException("Port type " + port.getClass().getSimpleName() + " is not supported");
        }
        return ports;
    }

    /**
     * Retrieves the ID of the bus the given port is connected to.
     *
     * @param port the port for which the connected bus ID is needed
     * @return the connected bus ID as a string
     */
    private String getConnectedBusId(Port port) {
        List<Port> substationPortList = getSortedSubstationPortList(port);
        String id = substationPortList.getFirst().getId();
        return busIdMap.get(id);
    }


    private void createAssets(List<EnergyAsset> assetList, ElectricityCommodity carrier) {
        AtomicInteger producerCounter = new AtomicInteger();
        AtomicInteger consumerCounter = new AtomicInteger();
        AtomicInteger storageCounter = new AtomicInteger();
        AtomicInteger conversionCounter = new AtomicInteger();
        assetList.stream().filter(a -> a instanceof Producer).forEach(producer -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, producer);
            Bus bus = getBus(carrier, producer);
            Generator generator = createProducer(voltageLevel, bus.getId(), (Producer) producer);
            carrierMap.put(generator.getId(), carrier);
            producerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Consumer).forEach(consumer -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, consumer);
            Bus bus = getBus(carrier, consumer);
            Load load = createConsumer(voltageLevel, bus.getId(), consumer);
            carrierMap.put(load.getId(), carrier);
            consumerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Storage).forEach(storage -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, storage);
            Bus bus = getBus(carrier, storage);
            Battery battery = createStorage(voltageLevel, bus.getId(), (Storage) storage);
            carrierMap.put(battery.getId(), carrier);
            storageCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof AbstractBasicConversion).forEach(conversion -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, conversion);
            Bus bus = getBus(carrier, conversion);
            createConversion(voltageLevel, carrier, bus.getId(), (AbstractBasicConversion) conversion);
            conversionCounter.incrementAndGet();
        });
        log.debug("Created {} producers, {} consumers, {} storages and {} conversions", producerCounter.get(), consumerCounter.get(), storageCounter.get(), conversionCounter.get());
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
    private Bus getBus(ElectricityCommodity carrier, EnergyAsset asset) {
        Optional<Port> firstPortForCarrier = getFirstPortForCarrier(asset, carrier);
        if (firstPortForCarrier.isEmpty()) {
            throw new IllegalStateException(String.format("Asset %s is not connected to carrier %s", asset.getName(), carrier.getName()));
        }
        List<Port> substationPortList = getSortedSubstationPortList(firstPortForCarrier.get());
        String id = substationPortList.getFirst().getId();
        VoltageLevel voltageLevel = getVoltageLevel(firstPortForCarrier.get());
        String busId = "bus-" + id;
        Bus bus = busMap.get(busId);
        if (bus == null) {
            bus = voltageLevel.getBusBreakerView().newBus().setId(busId).setName(busId).add();
            busMap.put(busId, bus);
            busIdMap.putAll(substationPortList.stream().collect(Collectors.toMap(Port::getId, p -> busId)));
        }
        return bus;
    }

    private VoltageLevel getVoltageLevel(ElectricityCommodity carrier, EnergyAsset asset) {
        Optional<Port> firstPortForCarrier = getFirstPortForCarrier(asset, carrier);
        if (firstPortForCarrier.isEmpty()) {
            throw new IllegalStateException(String.format("Asset %s is not connected to carrier %s", asset.getName(), carrier.getName()));
        }
        return getVoltageLevel(firstPortForCarrier.get());
    }

    private VoltageLevel getVoltageLevel(Port port) {
        String id = getSortedSubstationPortList(port).getFirst().getId();
        String voltageLevelId = "voltageLevel-" + id;
        VoltageLevel voltageLevel = voltageLevelMap.get(voltageLevelId);
        if (voltageLevel == null) {
            voltageLevel = createVoltageLevel(port, id, voltageLevelId);
        }
        return voltageLevel;
    }

    private VoltageLevel createVoltageLevel(Port port, String id, String voltageLevelId) {
        VoltageLevel voltageLevel;
        Substation substation = network.newSubstation().setId("substation-" + id).setCountry(Country.NL).add();
        voltageLevel = substation.newVoltageLevel()
                .setId(voltageLevelId)
                .setNominalV(((ElectricityCommodity) port.getCarrier()).getVoltage() / 1000)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelMap.put(voltageLevelId, voltageLevel);
        return voltageLevel;
    }

    private Object createConversion(VoltageLevel voltageLevel, ElectricityCommodity carrier, String busId, AbstractBasicConversion conversion) {
        AtomicReference<Object> result = new AtomicReference<>();
        conversion.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).findFirst().ifPresent(port -> {
            if (port instanceof InPort) {
                Load load = voltageLevel.newLoad()
                        .setBus(busId)
                        .setConnectableBus(busId)
                        .setId("conversion-" + conversion.getId())
                        .setName(conversion.getName())
                        .setP0(0)
                        .setQ0(0)
                        .add();
                result.set(load);
                translationMap.put(conversion.getId(), load);
                reverseTranslationMap.put(load.getId(), conversion);
                carrierMap.put(load.getId(), carrier);
            } else if (port instanceof OutPort) {
                Generator generator = voltageLevel.newGenerator()
                        .setId("conversion-" + conversion.getId())
                        .setName(conversion.getName())
                        .setBus(busId)
                        .setConnectableBus(busId)
                        // Do we need to convert based on port???
                        .setMaxP(conversion.getPower())
                        .setMinP(0)
                        .setTargetP(0)
                        .setTargetQ(0)
                        .setVoltageRegulatorOn(false)
                        .add();
                result.set(generator);
                translationMap.put(conversion.getId(), generator);
                reverseTranslationMap.put(generator.getId(), conversion);
                carrierMap.put(generator.getId(), carrier);
            } else {
                throw new IllegalStateException("Port type " + port + " is not supported in loadflow");
            }
        });
        return result.get();
    }

    private Battery createStorage(VoltageLevel voltageLevel, String busId, Storage storage) {
        if (storage.getPort().size() > 1) {
            throw new IllegalStateException("Batteries with multiple ports not supported in loadflow");
        }
        Battery battery = voltageLevel.newBattery()
                .setBus(busId)
                .setConnectableBus(busId)
                .setId("storage-" + storage.getId())
                .setName(storage.getName())
                .setMaxP(storage.getMaxChargeRate() / 1000)
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .add();
        translationMap.put(storage.getId(), battery);
        reverseTranslationMap.put(battery.getId(), storage);

        log.trace("Asset {} is implemented as a storage: {}", storage.getClass().getInterfaces()[0].getSimpleName(), battery.toString());
        return battery;
    }

    private Load createConsumer(VoltageLevel voltageLevel, String busId, EnergyAsset consumer) {
        if (consumer.getPort().size() > 1) {
            throw new IllegalStateException("Consumers with multiple ports not supported in loadflow");
        }
        Load load = voltageLevel.newLoad()
                .setBus(busId)
                .setConnectableBus(busId)
                .setId("consumer-" + consumer.getId())
                .setName(consumer.getName())
                .setP0(0)
                .setQ0(0)
                .add();
        translationMap.put(consumer.getId(), load);
        reverseTranslationMap.put(load.getId(), consumer);
        log.trace("Asset {} is implemented as a load: {}", consumer.getClass().getInterfaces()[0].getSimpleName(), load.toString());
        return load;
    }

    private Generator createProducer(VoltageLevel voltageLevel, String busId, Producer producer) {
        if (producer.getPort().size() > 1) {
            throw new IllegalStateException("Producers with multiple ports not supported in loadflow");
        }

        Generator generator = voltageLevel.newGenerator()
                .setId("producer-" + producer.getId())
                .setName(producer.getName() + "-" + producer.getName())
                .setMaxP(Math.abs(producer.getPower()))
                .setMinP(0)
                .setMaxP(producer.getPower() / 1000)
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
        log.trace("Asset {} is implemented as a generator: {}", producer.getClass().getInterfaces()[0].getSimpleName(), generator.toString());
        translationMap.put(producer.getId(), generator);
        reverseTranslationMap.put(generator.getId(), producer);
        return generator;
    }

    String getBusId(String portId) {
        return busIdMap.get(portId);
    }

    private void assignPowerValues(IObservation observation) {
        String assetId = observation.getTags().get("assetId");
        String assetName = observation.getTags().get("assetName");
        Double powerVal = (Double) observation.getValues().get("allocationPower");
        Object o = translationMap.get(assetId);
        if (powerVal == null || o == null) {
            return;
        }
        // Set the initial variant so we can modify the power values
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        // Powsybl default unit is kW, so we divide by 1000
        double power = Math.abs(powerVal / 1000);
        if (o instanceof Load load) {
            load.setP0(power).setQ0(0);
        } else if (o instanceof Generator generator) {
            generator.setTargetP(power);
        } else if (o instanceof Battery battery) {
            battery.setTargetP(power);
        }
    }

    public void run(EssimTime timestamp) {
        // Update the variant ID for the simulation
        updateVariantId();
        LocalDateTime time = timestamp.getTime();
        ZoneId zoneId = ZoneId.systemDefault();
        network.setCaseDate(time.atZone(zoneId));
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setUseReactiveLimits(false);
        OpenLoadFlowParameters.create(parameters)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED);
        OpenLoadFlowProvider loadFlowProvider = new OpenLoadFlowProvider();
        LoadFlow.Runner loadFlowRunner = new LoadFlow.Runner(loadFlowProvider);

        loadFlowRunner.run(network, parameters);
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
        observations.forEach(this::assignPowerValues);
        status = SimulationStatus.STARTED;
        run(timestamp);
        HashMap<String, Observation> observationMap = new HashMap<>();

        final TopologyVisitor visitor = new DefaultTopologyVisitor() {
            @Override
            public void visitGenerator(Generator generator) {
                Observation observation = createObservation(generator.getId(), generator.getTerminal(), timestamp);
                observationMap.put(observation.getTags().get("assetId"), observation);
            }

            @Override
            public void visitLoad(Load load) {
                Observation observation = createObservation(load.getId(), load.getTerminal(), timestamp);
                observationMap.put(observation.getTags().get("assetId"), observation);
            }

            @Override
            public void visitBattery(Battery battery) {
                Observation observation = createObservation(battery.getId(), battery.getTerminal(), timestamp);
                observationMap.put(observation.getTags().get("assetId"), observation);
            }

            @Override
            public void visitLine(Line line, TwoSides side) {
                Carrier carrier = carrierMap.get(line.getId());
                Observation.ObservationBuilder builder = createObservationBuilder(timestamp, reverseTranslationMap.get(line.getId()), carrier);
                double p1 = Math.abs(line.getTerminal(TwoSides.TWO).getP());
                double p2 = Math.abs(line.getTerminal(TwoSides.ONE).getP());
                double p = p1 > p2 ? p1 - p2 : p2 - p1;
                double q1 = Math.abs(line.getTerminal(TwoSides.TWO).getP());
                double q2 = Math.abs(line.getTerminal(TwoSides.ONE).getP());
                double q = q1 > q2 ? q1 - q2 : q2 - q1;
                Observation observation = builder.value("loadflowPower", p * 1000)
                        .value("loadflowReactivePower", q * 1000)
                        .value("loadflowCurrent", line.getTerminal(side).getI())
                        .build();
                observationMap.put(observation.getTags().get("assetId"), observation);
                super.visitLine(line, side);
            }
        };
        voltageLevelMap.values().forEach(voltageLevel -> voltageLevel.visitEquipments(visitor));

        for (var i = 0; i < observations.size(); i++) {
            Observation observation = observations.get(i);
            Observation.ObservationBuilder builder = observation.toBuilder();
            Observation lfObservation = observationMap.get(observation.getTags().get("assetId"));
            if (lfObservation != null) {
                builder.values(lfObservation.getValues());
            }
            observations.set(i, builder.build());
        }
        return observations;
    }

    private Observation createObservation(String id, Terminal terminal, EssimTime timestamp) {
        Carrier carrier = carrierMap.get(id);
        Observation.ObservationBuilder builder = createObservationBuilder(timestamp, reverseTranslationMap.get(id), carrier);
        Observation observation = builder.value("loadflowPower", terminal.getP() * 1000)
                .value("loadflowReactivePower", terminal.getQ() * 1000)
//                .value("loadflowCurrent", terminal.getI())
                .build();
        return observation;
    }
}
