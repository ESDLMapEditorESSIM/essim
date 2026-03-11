package nl.tno.essim.observation.consumers;

import esdl.*;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PostgresPortProfileProcessor implements java.util.function.Consumer<Port> {
    private final String simulationId;
    private final EssimSimulation simulation;
    private final DatabaseConfiguration dbc;
    private final QuantityAndUnitReference quantityAndUnitReference;
    private final String solverIdentifier;

    public PostgresPortProfileProcessor(String simulationId, EssimSimulation simulation, EnergySystem energySystem, String solverIdentifier) {
        this.solverIdentifier = solverIdentifier;
        this.simulationId = simulationId;
        this.simulation = simulation;
        dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, simulation.getPostgresURL(), DatabaseTypeEnum.POSTGRESQL);
        quantityAndUnitReference = getQuantityAndUnitReference(energySystem);
    }

    @NotNull
    public static QuantityAndUnitReference getQuantityAndUnitReference(EnergySystem energySystem) {
        QuantityAndUnitType quantityAndUnitType = EsdlFactory.eINSTANCE.createQuantityAndUnitType();
        quantityAndUnitType.setId(UUID.randomUUID().toString());
        quantityAndUnitType.setUnit(UnitEnum.JOULE);
        quantityAndUnitType.setDescription("Energy in J");
        quantityAndUnitType.setMultiplier(MultiplierEnum.NONE);
        quantityAndUnitType.setPerUnit(UnitEnum.NONE);
        quantityAndUnitType.setPhysicalQuantity(PhysicalQuantityEnum.ENERGY);
        quantityAndUnitType.setPerTimeUnit(TimeUnitEnum.NONE);
        quantityAndUnitType.setPerMultiplier(MultiplierEnum.NONE);
        quantityAndUnitType.setPerScope(QuantityAndUnitScopeEnum.UNDEFINED);
        if (energySystem.getEnergySystemInformation().getQuantityAndUnits() == null) {
            energySystem.getEnergySystemInformation().setQuantityAndUnits(EsdlFactory.eINSTANCE.createQuantityAndUnits());
            energySystem.getEnergySystemInformation().getQuantityAndUnits().setId(UUID.randomUUID().toString());
        }
        energySystem.getEnergySystemInformation().getQuantityAndUnits().getQuantityAndUnit().add(quantityAndUnitType);
        QuantityAndUnitReference quantityAndUnitReference = EsdlFactory.eINSTANCE.createQuantityAndUnitReference();
        quantityAndUnitReference.setReference(quantityAndUnitType);
        return quantityAndUnitReference;
    }

    @Override
    public void accept(Port port) {
        String scenarioID = simulation.getScenarioID();
        DataTableProfile dataTableProfile = EsdlFactory.eINSTANCE.createDataTableProfile();
        dataTableProfile.setConfiguration(dbc);
        dataTableProfile.setId(UUID.randomUUID().toString());
        dataTableProfile.setTableName(PostgresObservationConsumer.convertToValidPostgresColumnName(solverIdentifier));
        dataTableProfile.setSchema(PostgresObservationConsumer.convertToValidPostgresColumnName(scenarioID));
        dataTableProfile.setColumnName("allocationenergy");
        dataTableProfile.setDatetimeColumnName("time");
        dataTableProfile.setFilter("tags->>'assetName' = '" + port.getEnergyasset().getName() + "' AND tags->>'simulationRun' = '" + simulationId + "'");
        dataTableProfile.setProfileQuantityAndUnit(quantityAndUnitReference);
        dataTableProfile.setStartDate(simulation.getStartDate());
        dataTableProfile.setEndDate(simulation.getEndDate());
        dataTableProfile.setMultiplier(1);
        port.getProfile().add(dataTableProfile);

    }
}
