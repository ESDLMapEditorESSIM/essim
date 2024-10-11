package nl.tno.essim.loadflow;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.*;
import esdl.*;
import lombok.EqualsAndHashCode;
import nl.tno.essim.commons.Commons;

import java.util.*;
import java.util.stream.Collectors;

public class EssimLoadFlowUtil {


    @EqualsAndHashCode
    static class AssetPair {
        EnergyAsset first;
        EnergyAsset second;

        @EqualsAndHashCode.Exclude
        Set<Transport> connections = new HashSet<>();

        public AssetPair(EnergyAsset asset) {
            this.first = asset;
            this.second = asset;
        }

        public AssetPair(EnergyAsset asset1, EnergyAsset asset2) {
            if (asset1.getId().compareTo(asset2.getId()) > 0) {
                this.first = asset1;
                this.second = asset2;
            } else {
                this.first = asset2;
                this.second = asset1;

            }
        }

        public void addAsset(EnergyAsset asset) {
            if (first != second) {
                throw new IllegalStateException("Both assets have already been added");
            }
            if (asset.getId().compareTo(first.getId()) > 0) {
                this.first = asset;
            } else {
                this.second = asset;
            }
        }

        public void addConnection(Transport connection) {
            connections.add(connection);
        }
    }

    public static String getElementId(Asset asset) {
        return asset.getId() + "-" + asset.getClass().getSimpleName();
    }

    public static void createNetwork(String networkName, Asset rootAsset, List<EnergyAsset> assetList) {
        Network network = Network.create(networkName, "code");
         /* We may need a substation, because many elements can only be created in a substation
          * Substations contain voltagelevels and transformers
          * Voltagelevels contain generators, loads and batteries. And buses and breakers (in busbreaker view getBusBreakerView().newBus())
          *                                                        OR nodes, but I don't understand nodes yet
          * Network contains substations and lines and voltage levels. And also subnetworks....
          * So, for now we create:
          * - a substation if we encounter a transformer
          * - a bus for each device port
          * - A generator/load/battery per device. If a device has multiple ports, create a substation
          * - A line for each line
          * - A voltagelevel per carrier
          */
//        network.newLine().setNode1()


        /*
         * - First create a voltagelevel per carrier
         * - Then iterate through the elements connected to the carrier
         */

        HashMap<Port, Bus> portBusMap = new HashMap<>();
        List<EnergyAsset> elements = new ArrayList<>();
        for (EnergyAsset asset : assetList) {
            if (asset instanceof Producer) {
                Producer producer = (Producer) asset;
                elements.add(producer);
                Bus b = EssimNetworkFactory.createBus(network, producer.getId());
                producer.getPort().forEach(p -> portBusMap.put(p, b));
                Generator g = b.getVoltageLevel()
                        .newGenerator()
                        .setId(EssimLoadFlowUtil.getElementId(asset))
                        .setBus(b.getId())
                        .setConnectableBus(b.getId())
                        .setEnergySource(EnergySource.OTHER)
                        .setMinP(0)
                        .setMaxP(producer.getPower())
//                        .setTargetP(p)
                        .setTargetV(1)
                        .setVoltageRegulatorOn(true)
                        .add();
//                g.getTerminal().setP(-p).setQ(0);

            } else if (asset instanceof Consumer) {
                Consumer consumer = (Consumer) asset;
                elements.add(consumer);
                Bus b = EssimNetworkFactory.createBus(network, consumer.getId());
                consumer.getPort().forEach(p -> portBusMap.put(p, b));
                Load l = b.getVoltageLevel().newLoad()
                        .setId(EssimLoadFlowUtil.getElementId(asset))
                        .setBus(b.getId())
                        .setConnectableBus(b.getId())
//                        .setP0(p)
                        .setQ0(0)
                        .add();
//                l.getTerminal().setP(p).setQ(0);
            } else if (asset instanceof Storage) {
                Storage storage = (Storage) asset;
                elements.add(storage);
                Bus b = EssimNetworkFactory.createBus(network, asset.getId());
                storage.getPort().forEach(p -> portBusMap.put(p, b));
                b.getVoltageLevel().newBattery()
                        .setId(EssimLoadFlowUtil.getElementId(asset))
                        .setBus(b.getId())
                        .setConnectableBus(b.getId())
                        .setMinP(-storage.getMaxChargeRate())
                        .setMaxP(storage.getMaxDischargeRate())
//                        .setTargetP(-1)
//                        .setTargetQ(-0.1)
                        .add();

//            } else if (asset instanceof Transformer) { // TODO
            } else if (asset instanceof Transport) {
            }
        }

        HashMap<AssetPair, Line> lineMap = new HashMap<>();
        for (EnergyAsset asset : elements) {

//            asset.getPort()
        }
    }

    private static HashSet<AssetPair> getAssetPairs(EnergyAsset asset) {
        HashSet<AssetPair> pairs = new HashSet<>();
        HashMap<EnergyAsset, Port> allConnectedAssets = Commons.findAllConnectedAssets(asset);
        for (var a : allConnectedAssets.entrySet()) {
            if (a.getKey() instanceof Transport) {
                pairs.addAll(getAssetPairs(asset, (Transport) a.getKey(), new HashSet<>()));
            } else {
                pairs.add(new AssetPair(asset, a.getKey()));
            }
        }
        return pairs;
    }

    private static HashSet<AssetPair> getAssetPairs(EnergyAsset asset, Transport transport, HashSet<Transport> handled) {
        HashSet<AssetPair> pairs = new HashSet<>();
        handled.add(transport);
        HashMap<EnergyAsset, Port> connectedAssets = Commons.findAllConnectedAssets(transport);
        connectedAssets.remove(asset);
        for (var a : connectedAssets.entrySet()) {
            if (a.getKey() instanceof Transport && !handled.contains(a.getKey())) {

            } else {
                AssetPair e = new AssetPair(asset, a.getKey());
                e.addConnection(transport);
                pairs.add(e);
            }
        }
        return pairs;
    }

    public static Set<Port> findConnectedDevicePorts(Port port) {
        HashMap<Port, Boolean> ports = new HashMap<>();
        addConnectedDevicePorts(ports, port);
        return ports.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private static void addConnectedDevicePorts(HashMap<Port, Boolean> ports, Port port) {
        if (port instanceof InPort) {
            InPort inPort = (InPort) port;
            for (OutPort outPort : inPort.getConnectedTo()) {
                if (!ports.containsKey(outPort)) {
                    if (outPort.getEnergyasset() instanceof Transport) {
                        ports.put(outPort, false);
                        addConnectedDevicePorts(ports, outPort);
                    } else {
                        ports.put(outPort, true);
                    }
                }
            }
        } else if (port instanceof OutPort) {
            OutPort outPort = (OutPort) port;
            for (InPort inPort : outPort.getConnectedTo()) {
                if (!ports.containsKey(inPort)) {
                    if (inPort.getEnergyasset() instanceof Transport) {
                        ports.put(inPort, false);
                        addConnectedDevicePorts(ports, inPort);
                    } else {
                        ports.put(inPort, true);
                    }
                }
            }
        } else {
            throw new IllegalStateException("Port type " + port + " is not supported");
        }
    }
}
