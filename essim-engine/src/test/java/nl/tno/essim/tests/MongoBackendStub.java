/**
 * 
 */
package nl.tno.essim.tests;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.*;
import nl.tno.essim.mongo.IMongoBackend;

/**
 * @author werkmane
 *
 */
public class MongoBackendStub implements IMongoBackend {
	
	private Status simulationState;
	private String statusString;
	private EssimSimulation essimSimulation;
	private HashMap<String, String> metainfo = new HashMap<>();

	public MongoBackendStub() {
	}
	

	@Override
	public HashMap<String, IStatusProvider> getStatusMap() {
		return new HashMap<>();
	}

	@Override
	public void updateStatus(String status) {
		this.statusString = status;
	}

	@Override
	public String getStatus() {
		return statusString;
	}

	@Override
	public String addSimulation(EssimSimulation simulation) {
		this.essimSimulation = simulation;
		return UUID.randomUUID().toString();
	}

	@Override
	public void updateSimulationData(String simulationId, EssimSimulation simulationData) {
	}

	@Override
	public SimulationStatus getSimulationStatus(String simulationId) {
		SimulationStatusImpl status = new SimulationStatusImpl();
		status.setState(simulationState);
		status.setDescription("description");
		return status;
	}

	@Override
	public void updateSimulationStatus(String simulationId, Status type, String status) {
		this.simulationState = type;
		this.statusString = status;
	}

	@Override
	public List<EssimSimulation> getSimulations() {
		return List.of(essimSimulation);
		
	}

	@Override
	public EssimSimulation getSimulation(String simulationId) {
		return essimSimulation;
	}

	@Override
	public void stop() {
	}

	@Override
	public String getMetaInfo(String key) {
		return this.metainfo .get(key);
	}

	@Override
	public void updateMetaInfo(String key, String value) {
		this.metainfo.put(key, value);

	}

	@Override
	public void updateKPIForSimulation(String simulationId, String kpiId, String result, String status) {

	}

}
