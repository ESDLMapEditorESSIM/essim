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

package nl.tno.essim.transportsolver.nodes;

import esdl.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.Horizon;
import org.slf4j.MarkerFactory;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class HeatPumpNode extends AbstractBasicConversionNode {

    protected static final double DEFAULT_HP_COP = 3.5;
    protected double cop;
    protected HeatPump heatPump;
    private String hpName;

    @Builder(builderMethodName = "heatPumpNodeBuilder")
    public HeatPumpNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                        int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                        Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        this.heatPump = (HeatPump) asset;
        this.hpName = heatPump.getName() == null ? heatPump.getId() : heatPump.getName();

        this.cop = heatPump.getCOP();
        if (cop < Commons.eps) {
            cop = DEFAULT_HP_COP;
            log.warn(MarkerFactory.getMarker(simulationId), "CoP for HP {} is not defined! Defaulting to 3.5", hpName);
        }


        InPort eInputPort = null;
        InPort hInputPort = null;
        OutPort outputPort = null;
        for (Port port : conversion.getPort()) {
            if (port instanceof InPort) {
                if (port.getCarrier() instanceof HeatCommodity) {
                    hInputPort = (InPort) port;
                }
                else if (!(port.getCarrier() instanceof GasCommodity)) {
                    eInputPort = (InPort) port;
                }
            } else {
                outputPort = (OutPort) port;
            }
        }
        // We need to put the COP in the ratio map
        setMainPort(outputPort);
        getRatioMap().put(outputPort, 1.0);
        if (eInputPort == null) {
            throw new IllegalStateException("Couldn't find electricity input port in heat pump " + hpName);
        }
        getRatioMap().put(eInputPort, 1 / cop);
        if (hInputPort != null) {
            getRatioMap().put(hInputPort, 1 - (1 / cop));
        }
    }

}
