package nl.tno.essim.observation.consumers;

import esdl.*;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.profiles.DataTableProfileDataHandler;

import java.util.UUID;

public class InfluxDBPortProfileProcessor implements java.util.function.Consumer<Port> {
    private final String simulationId;
    private final EssimSimulation simulation;
    private final DatabaseConfiguration dbc;
    private final QuantityAndUnitReference quantityAndUnitReference;
    private final String solverIdentifier;

    public InfluxDBPortProfileProcessor(String simulationId, EssimSimulation simulation, EnergySystem energySystem, String solverIdentifier) {
        this.solverIdentifier = solverIdentifier;
        this.simulationId = simulationId;
        this.simulation = simulation;
        dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, simulation.getInfluxURL(), DatabaseTypeEnum.INFLUXDB);
        quantityAndUnitReference = PostgresPortProfileProcessor.getQuantityAndUnitReference(energySystem);
    }

    @Override
    public void accept(Port port) {
        String scenarioID = simulation.getScenarioID();
        dbc.setDatabase(scenarioID);
        DataTableProfile dataTableProfile = EsdlFactory.eINSTANCE.createDataTableProfile();
        dataTableProfile.setConfiguration(dbc);
        dataTableProfile.setId(UUID.randomUUID().toString());
        dataTableProfile.setTableName(solverIdentifier);
        dataTableProfile.setColumnName("allocationenergy");
        dataTableProfile.setDatetimeColumnName("time");
        dataTableProfile.setFilter("assetName='" + port.getEnergyasset().getName() + "' AND simulationRun='" + simulationId + "'");
        dataTableProfile.setProfileQuantityAndUnit(quantityAndUnitReference);
        dataTableProfile.setStartDate(simulation.getStartDate());
        dataTableProfile.setEndDate(simulation.getEndDate());
        dataTableProfile.setMultiplier(1);
        port.getProfile().add(dataTableProfile);

    }
}
