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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.SimulationStatus;
import nl.tno.essim.model.Status;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonCodecRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class MongoBackend implements IMongoBackend {

    private static IMongoBackend instance;
    private static final String ESSIM_DATABASE = "essimDB";
    private static final String ESSIM_COLL_NAME = "simCollection";
    private static final String ESSIM_META_COLL_NAME = "essimMetaInfo";
    private final MongoClient mongoClient;
    @Getter
    private final HashMap<String, IStatusProvider> statusMap;
    private final MongoDatabase essimDB;
    private final MongoCollection<EssimSimulation> essimCollection;
    private final MongoCollection<Document> essimMetaCollection;
    private String status;

    public synchronized static IMongoBackend getInstance() {
        return MongoBackend.instance;
    }

    public MongoBackend(String host, String port) {
        ServerAddress serverAddress = new ServerAddress(host, Integer.parseInt(port));
        mongoClient = MongoClients.create(new ConnectionString("mongodb://" + host + ":" + port));

        essimDB = mongoClient.getDatabase(ESSIM_DATABASE);
        testMongoConnection(serverAddress);

        com.mongodb.client.MongoCollection<?> collection = essimDB.getCollection(ESSIM_COLL_NAME, Document.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // StdDateFormat is ISO8601 since jackson 2.9
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

        JacksonCodecRegistry jacksonCodecRegistry = new JacksonCodecRegistry(mapper, collection.getCodecRegistry(), UuidRepresentation.JAVA_LEGACY);
        jacksonCodecRegistry.addCodecForClass(EssimSimulation.class);
        essimCollection = collection.withDocumentClass(EssimSimulation.class).withCodecRegistry(jacksonCodecRegistry);
        essimMetaCollection = essimDB.getCollection(ESSIM_META_COLL_NAME);

        updateStatus("Ready");

        statusMap = new HashMap<>();

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
            BsonDocument ping = new BsonDocument("ping", new BsonInt64(1));
            essimDB.runCommand(ping);
        } catch (MongoTimeoutException e) {
            log.error("Connection to MongoDB @ {} failed! Exiting!", serverAddress.toString());
            System.exit(1);
        }
        log.debug("Connection to MongoDB @ {} succeeded!", serverAddress.toString());
    }

    @Override
    public String addSimulation(EssimSimulation simulation) {
        InsertOneResult insertOneResult = essimCollection.insertOne(simulation);
        return insertOneResult.getInsertedId().asObjectId().getValue().toHexString();
    }

    @Override
    public void updateSimulationData(String simulationId, EssimSimulation simulationData) {
        simulationData.getAdditionalProperties().clear();
        essimCollection.replaceOne(Filters.eq("_id", new ObjectId(simulationId)), simulationData);
    }

    @Override
    public SimulationStatus getSimulationStatus(String simulationId) {
        EssimSimulation simulation = getSimulation(simulationId);
        return simulation == null ? null : simulation.getStatus();
    }

    @Override
    public void updateSimulationStatus(String simulationId, Status type, String status) {
        Document statusObj = new Document();
        statusObj.put("state", type);
        statusObj.put("description", status);
        Bson update = Updates.set("status", statusObj);
        new ObjectId(simulationId);
        essimCollection.updateOne(Filters.eq("_id", new ObjectId(simulationId)), update);
    }

    @Override
    public List<EssimSimulation> getSimulations() {
        List<EssimSimulation> simInstance = new ArrayList<>();
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
        try (MongoCursor<EssimSimulation> simIt = essimCollection.find(Filters.eq("_id", new ObjectId(simulationId))).iterator()) {
            return simIt.hasNext() ? simIt.next() : null;
        }
    }

    @Override
    public void stop() {
        mongoClient.close();
    }

    @Override
    public String getMetaInfo(String key) {
        Document valueObj = essimMetaCollection.find(Filters.eq("_id", key)).first();
        if (valueObj == null) {
            return null;
        }
        return valueObj.getString("value");
    }

    @Override
    public void updateMetaInfo(String key, String value) {
        essimMetaCollection.findOneAndUpdate(Filters.eq("_id", key), Updates.set("value", value));
    }

    @Override
    public void updateKPIForSimulation(String simulationId, String kpiId, String result, String status) {
        UpdateOptions options = new UpdateOptions()
                .arrayFilters(List.of(Filters.eq("ele.id", kpiId)));
        Bson filter = Filters.eq("_id", new ObjectId(simulationId));
        Bson set = Updates.combine(Updates.set("kpiModule.modules.$[ele].result", result), Updates.set("kpiModule.modules.$[ele].status", status));
        UpdateResult updateResult = essimCollection.updateOne(filter, set, options);
        log.debug("Result: {}", updateResult);
    }
}
