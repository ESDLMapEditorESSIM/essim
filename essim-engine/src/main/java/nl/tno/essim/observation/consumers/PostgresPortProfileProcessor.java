package nl.tno.essim.observation.consumers;

import java.util.UUID;

import esdl.DataTableProfile;
import esdl.DatabaseConfiguration;
import esdl.DatabaseTypeEnum;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.Port;
import esdl.QuantityAndUnitType;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.utils.ESDLUtil;

public class PostgresPortProfileProcessor implements java.util.function.Consumer<Port> {
	private final String simulationId;
	private final EssimSimulation simulation;
	private final DatabaseConfiguration dbc;
	private final String solverIdentifier;	
	private final QuantityAndUnitType energyInJouleQuantityAndUnitType;

	public PostgresPortProfileProcessor(String simulationId, EssimSimulation simulation, EnergySystem energySystem,
			String solverIdentifier) {
		this.solverIdentifier = solverIdentifier;
		this.simulationId = simulationId;
		this.simulation = simulation;
		dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, simulation.getPostgresURL(),
				DatabaseTypeEnum.POSTGRESQL);
		this.energyInJouleQuantityAndUnitType = ESDLUtil.getOrCreateEnergyInJouleQuantityAndUnit(energySystem);
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
		dataTableProfile.setFilter("tags->>'assetName' = '" + port.getEnergyasset().getName()
				+ "' AND tags->>'simulationRun' = '" + simulationId + "'");
		var qauReference = ESDLUtil.createQaUReference(energyInJouleQuantityAndUnitType);
		dataTableProfile.setProfileQuantityAndUnit(qauReference);
		dataTableProfile.setStartDate(simulation.getStartDate());
		dataTableProfile.setEndDate(simulation.getEndDate());
		dataTableProfile.setMultiplier(1);
		port.getProfile().add(dataTableProfile);

	}

}
