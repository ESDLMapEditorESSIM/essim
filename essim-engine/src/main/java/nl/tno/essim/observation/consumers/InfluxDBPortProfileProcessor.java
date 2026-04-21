package nl.tno.essim.observation.consumers;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.utils.ESDLUtil;

import java.util.UUID;

@Slf4j
public class InfluxDBPortProfileProcessor implements java.util.function.Consumer<Port> {
    private final String simulationId;
    private final EssimSimulation simulation;
    private final DatabaseConfiguration dbc;
    private final QuantityAndUnitType energyInJouleQuantityAndUnitType;
    private final String solverIdentifier;

    public InfluxDBPortProfileProcessor(String simulationId, EssimSimulation simulation, EnergySystem energySystem, String solverIdentifier) {
        this.solverIdentifier = solverIdentifier;
        this.simulationId = simulationId;
        this.simulation = simulation;
        this.energyInJouleQuantityAndUnitType = ESDLUtil.getOrCreateEnergyInJouleQuantityAndUnit(energySystem);
        dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, simulation.getInfluxURL(), DatabaseTypeEnum.INFLUXDB);
    }

    @Override
    public void accept(Port port) {
    	log.debug("Adding InfluxDB profile to port " + port.getEnergyasset().getName() + "." + port.eClass().getName() + "[name=" + port.getName() + "]");
        String scenarioID = simulation.getScenarioID();
        dbc.setDatabase(scenarioID);
        DataTableProfile dataTableProfile = EsdlFactory.eINSTANCE.createDataTableProfile();
        dataTableProfile.setConfiguration(dbc);
        dataTableProfile.setId(UUID.randomUUID().toString());
        dataTableProfile.setTableName(solverIdentifier);
        dataTableProfile.setColumnName("allocationEnergy");
        dataTableProfile.setDatetimeColumnName("time");
        dataTableProfile.setFilter("assetName='" + port.getEnergyasset().getName() + "' AND simulationRun='" + simulationId + "'");
        dataTableProfile.setProfileQuantityAndUnit(ESDLUtil.createQaUReference(energyInJouleQuantityAndUnitType));
        dataTableProfile.setStartDate(simulation.getStartDate());
        dataTableProfile.setEndDate(simulation.getEndDate());
        dataTableProfile.setMultiplier(1);
        dataTableProfile.setProfileType(ProfileTypeEnum.OUTPUT);
        port.getProfile().add(dataTableProfile);

    }
}
