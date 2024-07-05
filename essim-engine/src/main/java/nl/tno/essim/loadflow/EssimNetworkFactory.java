package nl.tno.essim.loadflow;

import com.powsybl.iidm.network.*;

public class EssimNetworkFactory {

    public static Bus createBus(Network network, String id) {
        return createBus(network, id, 1d);
    }

    public static Bus createBus(Network network, String id, double nominalV) {
        Substation s = network.getSubstation(id + "_s");
        if (s == null) {
            s = network.newSubstation()
                    .setId(id + "_s")
                    .setCountry(Country.NL)
                    .add();
        }
        VoltageLevel vl = s.newVoltageLevel()
                .setId(id + "_vl")
                .setNominalV(nominalV)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        return vl.getBusBreakerView().newBus()
                .setId(id)
                .add();
    }

    public static Bus createBus(Network network, String substationId, String id) {
        Substation s = network.getSubstation(substationId);
        if (s == null) {
            s = network.newSubstation()
                    .setId(substationId)
                    .setCountry(Country.FR)
                    .add();
        }
        VoltageLevel vl = s.newVoltageLevel()
                .setId(id + "_vl")
                .setNominalV(1)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        return vl.getBusBreakerView().newBus()
                .setId(id)
                .add();
    }

    public static Bus createOtherBus(Network network, String id, String voltageLevelId) {
        VoltageLevel vl = network.getVoltageLevel(voltageLevelId);
        return vl.getBusBreakerView().newBus()
                .setId(id)
                .add();
    }

    public static Generator createGenerator(Bus b, String id, double p) {
        return createGenerator(b, id, p, 1);
    }

    public static Generator createGenerator(Bus b, String id, double p, double v) {
        Generator g = b.getVoltageLevel()
                .newGenerator()
                .setId(id)
                .setBus(b.getId())
                .setConnectableBus(b.getId())
                .setEnergySource(EnergySource.OTHER)
                .setMinP(0)
                .setMaxP(2 * p)
                .setTargetP(p)
                .setTargetV(v)
                .setVoltageRegulatorOn(true)
                .add();
        g.getTerminal().setP(-p).setQ(0);
        return g;
    }

    public static Generator createGenerator2(Bus b, String id, double p, double v) {
        Generator g = b.getVoltageLevel()
                .newGenerator()
                .setId(id)
                .setBus(b.getId())
                .setConnectableBus(b.getId())
                .setEnergySource(EnergySource.OTHER)
                .setMinP(0)
                .setMaxP(2 * p)
                .add();
        return g;
    }

    public static Load createLoad(Bus b, String id, double p) {
        return createLoad(b, id, p, 0);
    }

    public static Load createLoad(Bus b, String id, double p, double q) {
        Load l = b.getVoltageLevel().newLoad()
                .setId(id)
                .setBus(b.getId())
                .setConnectableBus(b.getId())
                .setP0(p)
                .setQ0(q)
                .add();
        l.getTerminal().setP(p).setQ(q);
        return l;
    }

    public static Line createLine(Network network, Bus b1, Bus b2, String id, double x) {
        return createLine(network, b1, b2, id, 0, x);
    }

    public static Line createLine(Network network, Bus b1, Bus b2, String id, double r, double x) {
        return network.newLine()
                .setId(id)
                .setBus1(b1.getId())
                .setConnectableBus1(b1.getId())
                .setBus2(b2.getId())
                .setConnectableBus2(b2.getId())
                .setR(r)
                .setX(x)
                .add();
    }

}
