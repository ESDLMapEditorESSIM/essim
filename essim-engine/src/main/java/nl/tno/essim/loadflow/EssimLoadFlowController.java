package nl.tno.essim.loadflow;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Battery;
import esdl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class EssimLoadFlowController {
    private final Map<Asset, Object> translationMap = new HashMap<>();

    public void createNetwork(String networkName, Asset rootAsset, List<EnergyAsset> assetList) {
        Network network = Network.create(networkName, "code");
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
        assetList.stream().filter(carrier -> carrier instanceof ElectricityCommodity).forEach(carrier -> {
            VoltageLevel voltageLevel = network.newVoltageLevel()
                    .setId(carrier.getId())
                    .setNominalV(((ElectricityCommodity) carrier).getVoltage())
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .setName(carrier.getName())
                    .add();
            translationMap.put(carrier, voltageLevel);
            // Create the network elements for the connected assets
            List<EnergyAsset> connectedAssets = assetList.stream().filter(a -> !a.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).toList().isEmpty()).toList();
            createAssets(connectedAssets, (ElectricityCommodity) carrier, voltageLevel);
            // TODO create transformers!!

            // Create the cables
            createLines(connectedAssets, network);
        });
    }

    private void createLines(List<EnergyAsset> connectedAssets, Network network) {
        connectedAssets.stream().filter(a -> a instanceof ElectricityCable).forEach(cable -> {
            if (cable.getPort().size() != 2) {
                throw new IllegalStateException("Electricity cable with" + cable.getPort().size() + " ports not allowed!");
            }
            ((ElectricityCable) cable).getLength();
            Iterator<Port> iterator = cable.getPort().iterator();
            Port port1 = iterator.next();
            Port port2 = iterator.next();
            String cableTypeName = cable.getAssetType();
            if (cableTypeName == null || cableTypeName.isEmpty()) {
                cableTypeName = "NA2XS2Y 1x95 RM/25 12/20 kV";
                log.warn("No asset type for cable {} ({}) assuming default type {}", cable.getName(), cable.getId(), cableTypeName);
            }
            CableSpec cableSpec = CableSpec.getCableSpec(cableTypeName);
            double x = ((ElectricityCable) cable).getLength() * cableSpec.getXOhmPerKm() / 1000.0;
            double r = ((ElectricityCable) cable).getLength() * cableSpec.getROhmPerKm() / 1000.0;

            network.newLine()
                    .setConnectableBus1(port1.getId())
                    .setConnectableBus2(port2.getId())
                    .setX(x)
                    .setR(r)
                    .add();
        });

    }

    private void createAssets(List<EnergyAsset> assetList, ElectricityCommodity carrier, VoltageLevel voltageLevel) {
        assetList.stream().filter(a -> a instanceof Producer).forEach(producer -> {
            translationMap.put(producer, createProducer(voltageLevel, (Producer) producer));
        });
        assetList.stream().filter(a -> a instanceof Consumer).forEach(consumer -> {
            translationMap.put(consumer, createConsumer(voltageLevel, consumer));
        });
        assetList.stream().filter(a -> a instanceof Storage).forEach(storage -> {
            translationMap.put(storage, createStorage(voltageLevel, storage));
        });
        assetList.stream().filter(a -> a instanceof AbstractBasicConversion).forEach(conversion -> {
            translationMap.put(conversion, createConversion(voltageLevel, carrier, (AbstractBasicConversion) conversion));
        });
    }

    private Object createConversion(VoltageLevel voltageLevel, ElectricityCommodity carrier, AbstractBasicConversion conversion) {
        AtomicReference<Object> result = new AtomicReference<>();
        conversion.getPort().stream().filter(p -> p.getCarrier().equals(carrier)).findFirst().ifPresent(port -> {
            String portId = port.getId();
            voltageLevel.getBusBreakerView().newBus().setId(portId).add();
            if (port instanceof InPort) {
                result.set(voltageLevel.newLoad()
                        .setBus(portId)
                        .setConnectableBus(portId)
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .add());
            } else if (port instanceof OutPort) {
                result.set(voltageLevel.newGenerator()
                        .setId(conversion.getId())
                        .setName(conversion.getName())
                        .setBus(portId)
                        .setConnectableBus(portId)
                        // Do we need to convert based on port???
                        .setMaxP(conversion.getPower())
                        .add());
            } else {
                throw new IllegalStateException("Port type " + port + " is not supported in loadflow");
            }
        });
        return result.get();
    }

    private Battery createStorage(VoltageLevel voltageLevel, EnergyAsset storage) {
        if (storage.getPort().size() > 1) {
            throw new IllegalStateException("Batteries with multiple ports not supported in loadflow");
        }
        String portId = storage.getPort().get(0).getId();
        voltageLevel.getBusBreakerView().newBus().setId(portId).add();
        Battery battery = voltageLevel.newBattery()
                .setBus(portId)
                .setConnectableBus(portId)
                .setId(storage.getId())
                .setName(storage.getName())
                .add();
        return battery;
    }

    private Load createConsumer(VoltageLevel voltageLevel, EnergyAsset consumer) {
        if (consumer.getPort().size() > 1) {
            throw new IllegalStateException("Consumers with multiple ports not supported in loadflow");
        }
        String portId = consumer.getPort().get(0).getId();
        voltageLevel.getBusBreakerView().newBus().setId(portId).add();
        Load load = voltageLevel.newLoad()
                .setBus(portId)
                .setConnectableBus(portId)
                .setId(consumer.getId())
                .setName(consumer.getName())
//                .setP0()
                .add();
        return load;
    }

    private Generator createProducer(VoltageLevel voltageLevel, Producer producer) {
        if (producer.getPort().size() > 1) {
            throw new IllegalStateException("Producers with multiple ports not supported in loadflow");
        }
        String portId = producer.getPort().get(0).getId();
        voltageLevel.getBusBreakerView().newBus().setId(portId).add();
        Generator generator = voltageLevel.newGenerator()
                .setId(producer.getId())
                .setName(producer.getName())
                .setMaxP(Math.abs(producer.getPower()))
                .setMinP(0)
//                .setTargetP()
                .setBus(portId)
                .add();
        return generator;
    }

}
