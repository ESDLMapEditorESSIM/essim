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

import java.util.List;

import esdl.Carrier;
import esdl.CoGeneration;
import esdl.ElectricityCommodity;
import esdl.EnergyAsset;
import esdl.HeatCommodity;
import esdl.OutPort;
import esdl.Port;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.Commons.Role;
import nl.tno.essim.time.Horizon;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Slf4j
public class CoGenerationNode extends AbstractBasicConversionNode {

    @Builder(builderMethodName = "coGenerationNodeBuilder")
    public CoGenerationNode(String simulationId, String nodeId, String address, String networkId, EnergyAsset asset,
                            String esdlString, int directionFactor, Role role, BidFunction demandFunction, double energy, double cost,
                            Node parent, Carrier carrier, List<Node> children, long timeStep, Horizon now, Port connectedPort) {
        super(simulationId, nodeId, address, networkId, asset, esdlString, directionFactor, role, demandFunction,
                energy, cost, parent, carrier, children, timeStep, now, connectedPort);
        CoGeneration coGenerationPlant = (CoGeneration) asset;
        controlStrategy = coGenerationPlant.getControlStrategy();
        String coGenName = (coGenerationPlant.getName() == null ? coGenerationPlant.getId()
                : coGenerationPlant.getName());
        power = coGenerationPlant.getPower();
        costInformation = coGenerationPlant.getCostInformation();

        double heatEfficiency = coGenerationPlant.getHeatEfficiency();
        if (heatEfficiency < Commons.eps) {
            heatEfficiency = Commons.DEFAULT_COGEN_H_EFF;
        }
        double electricalEfficiency = coGenerationPlant.getElectricalEfficiency();
        if (electricalEfficiency < Commons.eps) {
            electricalEfficiency = Commons.DEFAULT_COGEN_E_EFF;
        }

        for (Port port : coGenerationPlant.getPort()) {
            if (port instanceof OutPort) {
                if (port.getCarrier() instanceof HeatCommodity) {
                    getRatioMap().put(port, heatEfficiency);
                } else {
                    // If not heat, we assume it is electricity
                    getRatioMap().put(port, electricalEfficiency);
                    // TODO maybe this should depend on coGenerationPlant.getLeadCommodity()
                    setMainPort(port);
                }
            } else {
                getRatioMap().put(port, 1.0);
            }
        }

    }
}
