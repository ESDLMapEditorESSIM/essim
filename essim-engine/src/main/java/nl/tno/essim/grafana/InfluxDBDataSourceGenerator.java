package nl.tno.essim.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;

@Slf4j
public class InfluxDBDataSourceGenerator implements DataSourceGenerator {
    private final static String DS_HANDLE = "DS_ESSIM";
    private final String influxDBURL;
    private String databaseUid;

    InfluxDBDataSourceGenerator(String influxDBURL) {
        this.influxDBURL = influxDBURL;
    }

    @Override
    public String getDBName(String resourceName) {
        return resourceName;
    }

    @Override
    public ObjectNode createDataSource(GrafanaClient grafanaClient, String scenarioName) {
        String databaseName = scenarioName;
        ObjectNode dsInput = ESSimEngine.getObjectMapper().createObjectNode();
        try {
            if (databaseUid == null) {
                databaseUid = createDatabase(grafanaClient, databaseName);
            }
            dsInput.put("name", DS_HANDLE);
//            dsInput.put("label", databaseLabel);
            dsInput.put("uid", databaseUid);
            dsInput.put("description", "");
            dsInput.put("type", "datasource");
            dsInput.put("pluginId", "influxdb");
            dsInput.put("pluginName", "InfluxDB");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return dsInput;
    }

    @Override
    public void addDataSourceReference(ObjectNode node) {
        ObjectNode dsNode = ESSimEngine.getObjectMapper().createObjectNode();
        dsNode.put("uid", databaseUid);
        dsNode.put("type", "influxdb");
        node.set("datasource", dsNode);
    }

    @Override
    public String getJsonPath() {
        return "/json/influxdb/";
    }


    private String checkDatabase(GrafanaClient client, String databaseName) {
        try {
            String datasourcesString = client.get(DATASOURCES_URL);
            JsonNode databases = ESSimEngine.getObjectMapper().readTree(datasourcesString);
            for (Object dbObj : databases) {
                ObjectNode db = (ObjectNode) dbObj;
                String dbName = db.get("database").asText();

                if (dbName.equalsIgnoreCase(databaseName)) {
                    return db.get("uid").asText();
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse json", e);
        }

        return null;
    }

    private String createDatabase(GrafanaClient client, String database) throws JsonProcessingException {
        String databaseName = checkDatabase(client, database);

        if (databaseName == null) {
            ObjectNode dbJSON = ESSimEngine.getObjectMapper().createObjectNode();
            dbJSON.put("name", database);
            dbJSON.put("database", database);
            dbJSON.put("type", "influxdb");
            dbJSON.put("url", influxDBURL);
            dbJSON.put("access", "proxy");
            dbJSON.set("jsonData", ESSimEngine.getObjectMapper().createObjectNode().set("keepCookies", ESSimEngine.getObjectMapper().createArrayNode()));
            dbJSON.set("secureJsonFields", ESSimEngine.getObjectMapper().createArrayNode());

            log.debug("Sending to Grafana: {}", dbJSON);
            String response = client.post(DATASOURCES_URL, dbJSON.toString());
            log.debug("Response: {}", response);
            JsonNode db = ESSimEngine.getObjectMapper().readTree(response);
            return db.path("datasource").get("uid").asText();
        } else {
            log.trace("Database {} already exists and is called {}!", database, databaseName);
            return databaseName;
        }
    }
}
