package nl.tno.essim.loadflow;

import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.*;
import esdl.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class EssimLoadFlowController {
    /*
     * The translationMap maps ESDL assets to powsybl elements like generators and loads
     */
    private final Map<Asset, Object> translationMap = new HashMap<>();

    /*
     * The busMap maps ESDL port ids to powsybl bus ids. When creating a bus, the id of the first port that will be connected
     * to it will be the bus id.
     */
    private final Map<String, String> busMap = new HashMap<>();
    private final Map<Carrier, VoltageLevel> voltageLevelMap = new HashMap<>();
    private final Network network;

    public EssimLoadFlowController(String networkName) {
        network = Network.create(networkName, "code");
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

        /*
         * - First create a voltagelevel per carrier
         * - Then iterate through the assets connected to the carrier to create loads/batteries/generators
         * - Then iterate through the lines to connect the assets
         */
        Substation substation = network.newSubstation().setId(network.getId() + "-substation").add();

        carriers.forEach(carrier -> {
            VoltageLevel voltageLevel = substation.newVoltageLevel()
                    .setId(carrier.getId())
                    .setNominalV(((ElectricityCommodity) carrier).getVoltage())
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .setName(carrier.getName())
                    .add();
            voltageLevelMap.put(carrier, voltageLevel);
        });

        // Now we create the network per carrier
        carriers.forEach(carrier -> {
            List<EnergyAsset> connectedAssets = assetList.stream().filter(a -> !a.getPort().stream().filter(p -> p.getCarrier() != null && p.getCarrier().equals(carrier)).toList().isEmpty()).toList();
            createNetworkPerCarrier(carrier, connectedAssets);
        });

        // Then we create the transformers connecting the carriers
        assetList.stream().filter(a -> a instanceof Transformer).forEach(esdlTransformer -> {
//            Substation substation = network.newSubstation().setId(esdlTransformer.getId() + "-substation").add();
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
            TwoWindingsTransformer transformer = substation.newTwoWindingsTransformer()
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
        VoltageLevel voltageLevel = voltageLevelMap.get(carrier);
        AtomicInteger busCounter = new AtomicInteger();
        /*
         * We need to first create a port-bus-id mapping, where we map all the connected ports to a bus id
         * So we first create a bus for the explicit busses from the ESDL, and for the EConnections
         */
        connectedAssets.stream().filter(a -> a instanceof Bus || a instanceof EConnection).forEach(bus -> {
            AtomicReference<String> busId = new AtomicReference<>();
            bus.getPort().forEach(port -> {
                if (busId.get() == null) {
                    busId.set(port.getId());
                    createBus(voltageLevel, busId.get());
                    busCounter.incrementAndGet();
                }
                busMap.put(port.getId(), busId.get());
            });
        });

        /*
         * Now we create busses for implicit connections
         */
        connectedAssets.forEach(asset -> {
            asset.getPort().stream().forEach(port -> {
                String portId = port.getId();
                if (!busMap.containsKey(portId) && port.getCarrier().equals(carrier)) {
                    createBus(voltageLevel, portId);
                    busCounter.incrementAndGet();
                    busMap.put(portId, portId);
                    getConnectedPorts(port).forEach(p -> {
                        busMap.put(p.getId(), portId);
                    });
                }
            });
        });

        log.info("{} busses created", busCounter.get());

        // Create the network elements for the connected assets
        createAssets(connectedAssets, (ElectricityCommodity) carrier, voltageLevel);

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
            String busId1 = getBusId(getConnectedPortId(iterator.next()));
            String busId2 = getBusId(getConnectedPortId(iterator.next()));
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
                    .setX(x)
                    .setR(r)
                    .setId(cable.getId())
                    .add();
        });

    }

    EList<Port> getConnectedPorts(Port port) {
        EList<Port> ports = new BasicEList<>();
        if (port instanceof InPort) {
            EList<OutPort> connectedTo = ((InPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else if (port instanceof OutPort) {
            EList<InPort> connectedTo = ((OutPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else {
            throw new IllegalStateException("Port type " + port.getClass().getSimpleName() + " is not supported in loadflow");
        }
        return ports;
    }

    private String getConnectedPortId(Port port) {
        String busId = null;
        if (port instanceof InPort) {
            EList<OutPort> connectedTo = ((InPort) port).getConnectedTo();
            if (connectedTo.size() != 1) {
                throw new IllegalStateException("Inport " + port.getName() + " connected to " + connectedTo.size() + " outports");
            }
            busId = connectedTo.get(0).getId();
        } else if (port instanceof OutPort) {
            EList<InPort> connectedTo = ((OutPort) port).getConnectedTo();
            if (connectedTo.size() != 1) {
                throw new IllegalStateException("Outport " + port.getName() + " connected to " + connectedTo.size() + " inports");
            }
            busId = connectedTo.get(0).getId();
        } else {
            throw new IllegalStateException("Port type " + port.getClass().getSimpleName() + " is not supported in loadflow");
        }
        return busId;
    }

    private void createAssets(List<EnergyAsset> assetList, ElectricityCommodity carrier, VoltageLevel voltageLevel) {
        assetList.stream().filter(a -> a instanceof Producer).forEach(producer -> {
            translationMap.put(producer, createProducer(voltageLevel, (Producer) producer));
        });
        assetList.stream().filter(a -> a instanceof Consumer).forEach(consumer -> {
            translationMap.put(consumer, createConsumer(voltageLevel, consumer));
        });
        assetList.stream().filter(a -> a instanceof Storage).forEach(storage -> {
            translationMap.put(storage, createStorage(voltageLevel, (Storage) storage));
        });
        assetList.stream().filter(a -> a instanceof AbstractBasicConversion).forEach(conversion -> {
            translationMap.put(conversion, createConversion(voltageLevel, carrier, (AbstractBasicConversion) conversion));
        });
        assetList.stream().filter(a -> a instanceof Transformer).forEach(connection -> {
//            // TODO create single bus
////            netw
//
//            translationMap.put(connection, createConversion(voltageLevel, carrier, (AbstractBasicConversion) connection));
        });
    }

    private Object createConversion(VoltageLevel voltageLevel, ElectricityCommodity carrier, AbstractBasicConversion conversion) {
        AtomicReference<Object> result = new AtomicReference<>();
        conversion.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).findFirst().ifPresent(port -> {
            String busId = getBusId(port.getId());
            if (port instanceof InPort) {
                result.set(voltageLevel.newLoad()
                        .setBus(busId)
                        .setConnectableBus(busId)
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .add());
            } else if (port instanceof OutPort) {
                result.set(voltageLevel.newGenerator()
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .setBus(busId)
                        .setConnectableBus(busId)
                        // Do we need to convert based on port???
                        .setMaxP(conversion.getPower())
                        .add());
            } else {
                throw new IllegalStateException("Port type " + port + " is not supported in loadflow");
            }
        });
        return result.get();
    }

    private Battery createStorage(VoltageLevel voltageLevel, Storage storage) {
        if (storage.getPort().size() > 1) {
            throw new IllegalStateException("Batteries with multiple ports not supported in loadflow");
        }
        String busId = getBusId(storage.getPort().get(0).getId());
        Battery battery = voltageLevel.newBattery()
                .setBus(busId)
                .setConnectableBus(busId)
                .setId(storage.getId())
                .setName(storage.getName())
                .setMaxP(storage.getMaxChargeRate())
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .add();
        return battery;
    }

    private Load createConsumer(VoltageLevel voltageLevel, EnergyAsset consumer) {
        if (consumer.getPort().size() > 1) {
            throw new IllegalStateException("Consumers with multiple ports not supported in loadflow");
        }
        String busId = getBusId(consumer.getPort().get(0).getId());
        Load load = voltageLevel.newLoad()
                .setBus(busId)
                .setConnectableBus(busId)
                .setId(consumer.getId())
                .setName(consumer.getName())
                .setP0(0)
                .setQ0(0)
                .add();
        return load;
    }

    private Generator createProducer(VoltageLevel voltageLevel, Producer producer) {
        if (producer.getPort().size() > 1) {
            throw new IllegalStateException("Producers with multiple ports not supported in loadflow");
        }
        String busId = getBusId(producer.getPort().get(0).getId());
        Generator generator = voltageLevel.newGenerator()
                .setId(producer.getId())
                .setName(producer.getName() + "-" + producer.getName())
                .setMaxP(Math.abs(producer.getPower()))
                .setMinP(0)
                .setTargetP(0)
                .setTargetQ(0)
                .setBus(busId)
                .setConnectableBus(busId)
                .setVoltageRegulatorOn(false)
                .add();
        return generator;
    }

    String getBusId(String portId) {
        String prevId;
        String busId = portId;
        do {
            prevId = busId;
            busId = busMap.get(prevId);
        } while (busId != null && !busId.equals(prevId));
        return busId;
    }

    private Bus createBus(VoltageLevel voltageLevel, String portId) {
        return voltageLevel.getBusBreakerView().newBus().setId(portId).setName(portId).add();
    }

}
