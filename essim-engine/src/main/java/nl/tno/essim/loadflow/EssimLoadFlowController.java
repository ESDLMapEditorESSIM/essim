package nl.tno.essim.loadflow;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.serde.XMLExporter;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationProcessor;
import nl.tno.essim.observation.IObservationProvider;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class EssimLoadFlowController implements IObservationProcessor {
    /*
     * The translationMap maps ESDL assets ids to powsybl elements like generators and loads
     */
    private final Map<String, Object> translationMap = new HashMap<>();

    /*
     * The busMap maps ESDL port ids to powsybl bus ids. When creating a bus, the id of the first port that will be connected
     * to it will be the bus id.
     */
    private final Map<String, String> busIdMap = new HashMap<>();
    private final Map<String, Bus> busMap = new HashMap<>();
    private final Map<String, VoltageLevel> voltageLevelMap = new HashMap<>();
    private final Network network;
    private final String networkName;
    private final AtomicInteger simulationRunCounter = new AtomicInteger();

    public EssimLoadFlowController(String networkName) {
        network = Network.create(networkName, "code");
        this.networkName = networkName;
    }

    public void createNetwork(List<Carrier> carriers, List<EnergyAsset> assetList) {
        /* We may need a substation, because many elements can only be created in a substation
         * Substations contain voltagelevels and transformers
         * Voltagelevels contain generators, loads and batteries. And buses and breakers (in busbreaker view getBusBreakerView().newBus())
         *                                                        OR nodes, but I don't understand nodes yet
         * Network contains substations and lines and voltage levels. And also subnetworks....
         * So, for now we create:
         * - a substation if we encounter a transformer
         * - a bus for each device port
         * - A generator/load/battery per device.
         * - A line for each line
         * - A voltagelevel per carrier
         */

        // Now we create the network per carrier
        carriers.forEach(carrier -> {
            List<EnergyAsset> connectedAssets = assetList.stream().filter(a -> !a.getPort().stream().filter(p -> p.getCarrier() != null && p.getCarrier().equals(carrier)).toList().isEmpty()).toList();
            createNetworkPerCarrier(carrier, connectedAssets);
        });

        // Then we create the transformers connecting the carriers
        createTransformers(assetList);
        Exporter ex = new XMLExporter();
        ex.export(network, new Properties(), DataSource.fromPath(Paths.get("."), "essim-network-" + networkName + ".xml"));
        NetworkAreaDiagram.draw(network, Paths.get("essim-network-" + networkName + "-diagram.svg"));

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
        createLines(connectedAssets, network);
    }

    private void createLines(List<EnergyAsset> connectedAssets, Network network) {
        connectedAssets.stream().filter(a -> a instanceof ElectricityCable).forEach(cable -> {
            if (cable.getPort().size() != 2) {
                throw new IllegalStateException("Electricity cable with " + cable.getPort().size() + " ports not allowed!");
            }
            ((ElectricityCable) cable).getLength();
            Iterator<Port> iterator = cable.getPort().iterator();
            Port port1 = iterator.next();
            Port port2 = iterator.next();
            String busId1 = getBusId(getConnectedPortId(port1));
            String busId2 = getBusId(getConnectedPortId(port2));
            String cableTypeName = cable.getAssetType();
            if (cableTypeName == null || cableTypeName.isEmpty()) {
                cableTypeName = "243-AL1/39-ST1A 20.0";
                log.warn("No asset type for cable {} ({}) assuming default type '{}'", cable.getName(), cable.getId(), cableTypeName);
            }
            CableSpec cableSpec = CableSpec.getCableSpec(cableTypeName);
            log.debug("Creating cable of type {}", cableTypeName);
            double x = ((ElectricityCable) cable).getLength() * cableSpec.getXOhmPerKm() / 1000.0;
            double r = ((ElectricityCable) cable).getLength() * cableSpec.getROhmPerKm() / 1000.0;
            network.newLine()
                    .setConnectableBus1(busId1)
                    .setConnectableBus2(busId2)
                    .setVoltageLevel1(getVoltageLevel(port1).getId())
                    .setVoltageLevel2(getVoltageLevel(port2).getId())
                    .setX(x)
                    .setR(r)
                    .setId(cable.getId())
                    .add();
        });

    }

    List<Port> getSortedSubstationPortList(Port port) {
        EList<Port> ports = getConnectedPorts(port);
        ports.addAll(ports.stream().filter(p -> isSubstationInternalConnection(p.getEnergyasset())).flatMap(p -> getConnectedPorts(p).stream()).toList());
        ports.add(port);
        return ports.stream().distinct().sorted(Comparator.comparing(Port::getId)).toList();
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
     * Retrieves the ID of the port connected to the specified port. The specified port
     * can either be an instance of InPort or OutPort. If the specified port is connected
     * to more than one port, an IllegalStateException is thrown.
     *
     * @param port the port for which the connected port ID should be retrieved. It must be
     *             either an instance of InPort or OutPort.
     * @return the ID of the port connected to the specified port.
     * @throws IllegalStateException if the specified port is connected to more than one port
     *                               or if the port type is unsupported.
     */
    private String getConnectedPortId(Port port) {
        String portId = null;
        if (port instanceof InPort) {
            EList<OutPort> connectedTo = ((InPort) port).getConnectedTo();
            if (connectedTo.size() != 1) {
                throw new IllegalStateException("Inport " + port.getName() + " connected to " + connectedTo.size() + " outports");
            }
            portId = connectedTo.get(0).getId();
        } else if (port instanceof OutPort) {
            EList<InPort> connectedTo = ((OutPort) port).getConnectedTo();
            if (connectedTo.size() != 1) {
                throw new IllegalStateException("Outport " + port.getName() + " connected to " + connectedTo.size() + " inports");
            }
            portId = connectedTo.get(0).getId();
        } else {
            throw new IllegalStateException("Port type " + port.getClass().getSimpleName() + " is not supported in loadflow");
        }
        return portId;
    }


    private void createAssets(List<EnergyAsset> assetList, ElectricityCommodity carrier) {
        AtomicInteger producerCounter = new AtomicInteger();
        AtomicInteger consumerCounter = new AtomicInteger();
        AtomicInteger storageCounter = new AtomicInteger();
        AtomicInteger conversionCounter = new AtomicInteger();
        assetList.stream().filter(a -> a instanceof Producer).forEach(producer -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, producer);
            Bus bus = getBus(carrier, producer);
            translationMap.put(producer.getId(), createProducer(voltageLevel, bus.getId(), (Producer) producer));
            producerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Consumer).forEach(consumer -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, consumer);
            Bus bus = getBus(carrier, consumer);
            translationMap.put(consumer.getId(), createConsumer(voltageLevel, bus.getId(), consumer));
            consumerCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof Storage).forEach(storage -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, storage);
            Bus bus = getBus(carrier, storage);
            translationMap.put(storage.getId(), createStorage(voltageLevel, bus.getId(), (Storage) storage));
            storageCounter.incrementAndGet();
        });
        assetList.stream().filter(a -> a instanceof AbstractBasicConversion).forEach(conversion -> {
            VoltageLevel voltageLevel = getVoltageLevel(carrier, conversion);
            Bus bus = getBus(carrier, conversion);
            translationMap.put(conversion.getId(), createConversion(voltageLevel, carrier, bus.getId(), (AbstractBasicConversion) conversion));
            conversionCounter.incrementAndGet();
        });
        log.debug("Created {} producers, {} consumers, {} storages and {} conversions", producerCounter.get(), consumerCounter.get(), storageCounter.get(), conversionCounter.get());
    }

    private Bus getBus(ElectricityCommodity carrier, EnergyAsset asset) {
        Optional<Port> firstPortForCarrier = getFirstPortForCarrier(asset, carrier);
        if (firstPortForCarrier.isEmpty()) {
            throw new IllegalStateException(String.format("Asset %s is not connected with carrier %s", asset.getName(), carrier.getName()));
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
            throw new IllegalStateException(String.format("Asset %s is not connected with carrier %s", asset.getName(), carrier.getName()));
        }
        return getVoltageLevel(firstPortForCarrier.get());
    }

    private VoltageLevel getVoltageLevel(Port port) {
        String id = getSortedSubstationPortList(port).getFirst().getId();
        String voltageLevelId = "voltageLevel-" + id;
        VoltageLevel voltageLevel = voltageLevelMap.get(voltageLevelId);
        if (voltageLevel == null) {
            Substation substation = network.newSubstation().setId("substation-" + id).add();
            voltageLevel = substation.newVoltageLevel().setId(voltageLevelId).setNominalV(((ElectricityCommodity) port.getCarrier()).getVoltage() / 1000).setTopologyKind(TopologyKind.BUS_BREAKER).add();
            voltageLevelMap.put(voltageLevelId, voltageLevel);
        }
        return voltageLevel;
    }

    private Object createConversion(VoltageLevel voltageLevel, ElectricityCommodity carrier, String busId, AbstractBasicConversion conversion) {
        AtomicReference<Object> result = new AtomicReference<>();
        conversion.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).findFirst().ifPresent(port -> {
            if (port instanceof InPort) {
                result.set(voltageLevel.newLoad()
                        .setBus(busId)
                        .setConnectableBus(busId)
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .setP0(0)
                        .setQ0(0)
                        .add());
            } else if (port instanceof OutPort) {
                result.set(voltageLevel.newGenerator()
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .setBus(busId)
                        .setConnectableBus(busId)
                        // Do we need to convert based on port???
                        .setMaxP(conversion.getPower())
                        .setMinP(0)
                        .setTargetP(0)
                        .setTargetQ(0)
                        .add());
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
                .setId(storage.getId())
                .setName(storage.getName())
                .setMaxP(storage.getMaxChargeRate() / 1000)
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .add();
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
                .setId(consumer.getId())
                .setName(consumer.getName())
                .setP0(0)
                .setQ0(0)
                .add();
        log.trace("Asset {} is implemented as a load: {}", consumer.getClass().getInterfaces()[0].getSimpleName(), load.toString());
        return load;
    }

    private Generator createProducer(VoltageLevel voltageLevel, String busId, Producer producer) {
        if (producer.getPort().size() > 1) {
            throw new IllegalStateException("Producers with multiple ports not supported in loadflow");
        }

        Generator generator = voltageLevel.newGenerator()
                .setId(producer.getId())
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
        return generator;
    }

    String getBusId(String portId) {
        return busIdMap.get(portId);
    }

    @Override
    public void process(IObservationProvider source, IObservation observation) {
        String assetId = (String) observation.getTags().get("assetId");
//        Double power = Math.abs((Double) observation.getValues().get("allocationPower"));
        Double power = (Double) observation.getValues().get("allocationPower");
        Object o = translationMap.get(assetId);
        if (o == null) {
            return;
        }
        if (o instanceof Load) {
            ((Load) o).setP0(power / 1000);
        } else if (o instanceof Generator) {
            ((Generator) o).setTargetP(power / 1000);
        } else if (o instanceof Battery) {
            ((Battery) o).setTargetP(power / 1000);
        }

    }

    public void run() {
        final String variantId = "loadflowVariant-" + simulationRunCounter.incrementAndGet();
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, variantId);
        network.getVariantManager().setWorkingVariant(variantId);
        LoadFlowParameters parameters = new LoadFlowParameters()
                .setDc(true).setUseReactiveLimits(false)
                .setDistributedSlack(false);
        OpenLoadFlowParameters parametersExt = OpenLoadFlowParameters.create(parameters)
                .setSlackBusSelectionMode(SlackBusSelectionMode.FIRST);
        OpenLoadFlowProvider loadFlowProvider = new OpenLoadFlowProvider(new DenseMatrixFactory());
        LoadFlow.Runner loadFlowRunner = new LoadFlow.Runner(loadFlowProvider);


        loadFlowRunner.run(network, parameters);

    }


}
