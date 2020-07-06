package nl.tno.essim.mongo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	private String essimTaskId;

	public synchronized static MongoBackend getInstance() {
		return MongoBackend.instance;
	}

	@SuppressWarnings("deprecation")
	public MongoBackend(String host, String port) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);

		MongoClientOptions opts = MongoClientOptions.builder()
				.serverSelectionTimeout(2000)
				.codecRegistry(MongoClient.getDefaultCodecRegistry())
				.build();
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
		
		updateStatus("waiting");

		statusMap = new HashMap<String, IStatusProvider>();

		instance = this;
	}

	public void updateStatus(String status) {
		BasicDBObject entry = new BasicDBObject();
		entry.put("_id", essimTaskId);
		entry.put("status", status);
		essimMetaCollection.save(entry);
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
		String id = result.getDbObject()
				.get("_id")
				.toString();
		return id;
	}

	public void updateSimulationData(String simulationId, EssimSimulation simulationData) {
		try {
			Query query = DBQuery.is("_id", new ObjectId(simulationId));
			simulationData.getAdditionalProperties()
					.clear();
			essimCollection.update(query, simulationData);
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
					record.getAdditionalProperties()
							.clear();
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
				simInstance.getAdditionalProperties()
						.clear();
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
