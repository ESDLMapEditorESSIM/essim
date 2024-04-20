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

package nl.tno.essim.mongo;

import com.mongodb.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.*;
import org.bson.types.ObjectId;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class MongoBackend implements IMongoBackend {

    private static IMongoBackend instance;
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
    private String status;

    public synchronized static IMongoBackend getInstance() {
        return MongoBackend.instance;
    }

    @SuppressWarnings("deprecation")
    public MongoBackend(String host, String port) {
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

        updateStatus("Ready");

        statusMap = new HashMap<String, IStatusProvider>();

        instance = this;
    }

    @Override
    public void updateStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
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

    @Override
    public String addSimulation(EssimSimulation simulation) {
        WriteResult<EssimSimulation, String> result = essimCollection.save(simulation);
        String id = result.getDbObject().get("_id").toString();
        return id;
    }

    @Override
    public void updateSimulationData(String simulationId, EssimSimulation simulationData) {
        try {
            Query query = DBQuery.is("_id", new ObjectId(simulationId));
            simulationData.getAdditionalProperties().clear();
            essimCollection.update(query, simulationData);
        } catch (MongoException e) {
            log.error("Could not update simulation data", e);
        }
    }

    @Override
    public SimulationStatus getSimulationStatus(String simulationId) {
        SimulationStatus simStatus = null;
        try {
            Query query = DBQuery.is("_id", new ObjectId(simulationId));
            EssimSimulation simulation = essimCollection.findOne(query);
            simStatus = simulation.getStatus();
        } catch (MongoException e) {
            log.error("Could not get simulation status", e);
        }
        return simStatus;
    }

    @Override
    public void updateSimulationStatus(String simulationId, Status type, String status) {
        try {
            SimulationStatus statusObj = new SimulationStatusImpl();
            statusObj.setState(type);
            statusObj.setDescription(status);

            Query query = DBQuery.is("_id", new ObjectId(simulationId));
            essimCollection.update(query, DBUpdate.set("status", statusObj));
        } catch (MongoException e) {
            log.error("Could not update simulation status", e);
        }
    }

    @Override
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
            log.error("Could not get mongo simulations", e);
        }

        return simInstance;
    }

    @Override
    public EssimSimulation getSimulation(String simulationId) {
        EssimSimulation simInstance = null;
        try {
            Query query = DBQuery.is("_id", new ObjectId(simulationId));
            simInstance = essimCollection.findOne(query);
            if (simInstance != null) {
                simInstance.getAdditionalProperties().clear();
            }
        } catch (MongoException e) {
            log.error("Could not get mongo simulation", e);
        }

        return simInstance;
    }

    @Override
    public void stop() {
        mongoClient.close();
    }

    @Override
    public String getMetaInfo(String key) {
        DBObject valueObj = essimMetaCollection.findOne(key);
        if (valueObj == null) {
            return null;
        }
        return (String) valueObj.get("value");
    }

    @Override
    public void updateMetaInfo(String key, String value) {
        BasicDBObject entry = new BasicDBObject();
        entry.put("_id", key);
        entry.put("value", value);
        essimMetaCollection.save(entry);
    }

    @Override
    public List<KPIModuleInfo> getAllKpiModules() {
        List<KPIModuleInfo> kpis = new ArrayList<KPIModuleInfo>();
        try {
            for (KPIModuleInfo kpi : kpiCollection.find()) {
                kpi.getAdditionalProperties().clear();
                kpis.add(kpi);
            }
        } catch (MongoException e) {
            log.error("Could not get KPI modules", e);
        }
        return kpis;
    }

    @Override
    public KPIModule getKPIForSimulation(String simulationId, String kpiId) {
        KPIModule kpiModule = null;
        EssimSimulation simulation = getSimulation(simulationId);
        if (simulation != null) {
            RemoteKPIModule remoteKPIModule = simulation.getKpiModule();
            if (remoteKPIModule != null) {
                if (remoteKPIModule.getModules() != null && !remoteKPIModule.getModules().isEmpty()) {
                    for (KPIModule module : remoteKPIModule.getModules()) {
                        if (kpiId.equals(module.getId())) {
                            kpiModule = module;
                            break;
                        }
                    }
                }
            }
        }
        return kpiModule;
    }
}
