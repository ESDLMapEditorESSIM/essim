/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.mongo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.KPIModuleInfo;
import nl.tno.essim.model.SimulationStatus;
import nl.tno.essim.model.SimulationStatusImpl;
import nl.tno.essim.model.Status;

@Slf4j
public class MongoBackend {

	private static MongoBackend instance;
	private static final String KPI_DATABASE = "KPIModules";
	private static final String KPI_COLL_NAME = "KPIModulesColl";
	private static final String ESSIM_DATABASE = "essimDB";
	private static final String ESSIM_COLL_NAME = "simCollection";
	private static final String ESSIM_META_COLL_NAME = "essimMetaInfo";
	private MongoClient mongoClient;
	@Getter
	private HashMap<String, IStatusProvider> statusMap;
	private JacksonDBCollection<EssimSimulation, String> essimCollection;
	private DBCollection essimMetaCollection;
	private DB essimDB;
	private DB kpiDB;
	private JacksonDBCollection<KPIModuleInfo, String> kpiCollection;
	private String essimStatusKey;

	public synchronized static MongoBackend getInstance() {
		return MongoBackend.instance;
	}

	@SuppressWarnings("deprecation")
	public MongoBackend(String host, String port) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);

		MongoClientOptions opts = MongoClientOptions.builder().serverSelectionTimeout(2000)
				.codecRegistry(MongoClient.getDefaultCodecRegistry()).build();
		ServerAddress serverAddress = new ServerAddress(host, Integer.parseInt(port));
		mongoClient = new MongoClient(serverAddress, opts);
		essimDB = mongoClient.getDB(ESSIM_DATABASE);
		kpiDB = mongoClient.getDB(KPI_DATABASE);
		testMongoConnection(serverAddress);
		DBCollection dbCollection = essimDB.createCollection(ESSIM_COLL_NAME, null);
		essimCollection = JacksonDBCollection.wrap(dbCollection, EssimSimulation.class, String.class);
		essimMetaCollection = essimDB.createCollection(ESSIM_META_COLL_NAME, null);
		DBCollection kpiDBCollection = kpiDB.createCollection(KPI_COLL_NAME, null);
		kpiCollection = JacksonDBCollection.wrap(kpiDBCollection, KPIModuleInfo.class, String.class);

		String essimTaskId;
		try {
			essimTaskId = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			essimTaskId = UUID.randomUUID().toString();
		}
		essimStatusKey = essimTaskId + ".status";

		updateStatus("Ready");

		statusMap = new HashMap<String, IStatusProvider>();

		log.debug("This ESSIM instance is called {}", essimTaskId);
		instance = this;
	}

	public void updateStatus(String status) {
		updateMetaInfo(essimStatusKey, status);
	}

	public String getStatus() {
		return getMetaInfo(essimStatusKey);
	}

	public void removeStatus() {
		BasicDBObject remQuery = new BasicDBObject().append("_id", essimStatusKey);
		essimMetaCollection.remove(remQuery);
	}

	private void testMongoConnection(ServerAddress serverAddress) {
		try {
			essimDB.command("ping");
		} catch (MongoTimeoutException e) {
			log.error("Connection to MongoDB @ {} failed! Exiting!", serverAddress.toString());
			System.exit(1);
		}
		log.debug("Connection to MongoDB @ {} succeeded!", serverAddress.toString());
	}

	public String addSimulation(EssimSimulation simulation) {
		WriteResult<EssimSimulation, String> result = essimCollection.save(simulation);
		String id = result.getDbObject().get("_id").toString();
		return id;
	}

	public void updateSimulationData(String simulationId, EssimSimulation simulationData) {
		try {
			Query query = DBQuery.is("_id", new ObjectId(simulationId));
			simulationData.getAdditionalProperties().clear();
			essimCollection.update(query, simulationData);
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}
	}

	public SimulationStatus getSimulationStatus(String simulationId) {
		SimulationStatus simStatus = null;
		try {
			Query query = DBQuery.is("_id", new ObjectId(simulationId));
			EssimSimulation simulation = essimCollection.findOne(query);
			simStatus = simulation.getStatus();
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}
		return simStatus;
	}

	public void updateSimulationStatus(String simulationId, Status type, String status) {
		try {
			System.out.println(Thread.currentThread().getName() + " updating status " + type + " : " + status);
//			JSONObject statusJSON = new JSONObject();
//			statusJSON.put("State", type.name());
//			statusJSON.put("Sescription", status);
			SimulationStatus statusObj = new SimulationStatusImpl();
			statusObj.setState(type);
			statusObj.setDescription(status);

			Query query = DBQuery.is("_id", new ObjectId(simulationId));
			EssimSimulation simulation = essimCollection.findOne(query);
			if (simulation != null) {
				simulation.getAdditionalProperties().clear();
				simulation.setStatus(statusObj);
				essimCollection.update(query, simulation);
			}
//			BasicDBObject updateQuery = new BasicDBObject();
//			updateQuery.append("$set", new BasicDBObject().append("status", statusJSON.toString()));
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}
	}

	public List<EssimSimulation> getSimulations() {
		List<EssimSimulation> simInstance = new ArrayList<EssimSimulation>();
		try {
			for (EssimSimulation record : essimCollection.find()) {
				simInstance.add(record);
				if (record != null) {
					record.getAdditionalProperties().clear();
				}
			}
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}

		return simInstance;
	}

	public EssimSimulation getSimulation(String simulationId) {
		EssimSimulation simInstance = null;
		try {
			Query query = DBQuery.is("_id", new ObjectId(simulationId));
			simInstance = essimCollection.findOne(query);
			if (simInstance != null) {
				simInstance.getAdditionalProperties().clear();
			}
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}

		return simInstance;
	}

	public void stop() {
		mongoClient.close();
	}

	public String getMetaInfo(String key) {
		DBObject valueObj = essimMetaCollection.findOne(key);
		if (valueObj == null) {
			return null;
		}
		return (String) valueObj.get("value");
	}

	public void updateMetaInfo(String key, String value) {
		BasicDBObject entry = new BasicDBObject();
		entry.put("_id", key);
		entry.put("value", value);
		essimMetaCollection.save(entry);
	}

	public List<KPIModuleInfo> getAllKpiModules() {
		List<KPIModuleInfo> kpis = new ArrayList<KPIModuleInfo>();
		try {
			for (KPIModuleInfo kpi : kpiCollection.find()) {
				kpi.getAdditionalProperties().clear();
				kpis.add(kpi);
			}
		} catch (MongoException e) {
			log.error("MongoException occurred: {}", e.getMessage());
		}
		return kpis;
	}

}
