package nl.tno.essim.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;

@Slf4j
public class InfluxDBDataSourceGenerator implements DataSourceGenerator {
    private static final String DATASOURCES_URL = "/api/datasources";
    private final String influxDBURL;

    InfluxDBDataSourceGenerator(String influxDBURL) {
        this.influxDBURL = influxDBURL;
    }

    @Override
    public String getDBName(String resourceName) {
        return "\"" + resourceName + "\"";
    }

    @Override
    public ObjectNode getDataSource(GrafanaClient grafanaClient, String databaseName) {
        ObjectNode dsInput = ESSimEngine.getObjectMapper().createObjectNode();
        String databaseLabel = createDatabase(grafanaClient, databaseName);
        dsInput.put("name", DS_HANDLE);
        dsInput.put("label", databaseLabel);
        dsInput.put("description", "");
        dsInput.put("type", "datasource");
        dsInput.put("pluginId", "influxdb");
        dsInput.put("pluginName", "InfluxDB");

        return dsInput;
    }


    private String checkDatabase(GrafanaClient client, String databaseName) {
        try {
            String datasourcesString = client.get(DATASOURCES_URL);
            JsonNode databases = ESSimEngine.getObjectMapper().readTree(datasourcesString);
            for (Object dbObj : databases) {
                ObjectNode db = (ObjectNode) dbObj;
                String dbName = db.get("database").asText();

                if (dbName.contains(databaseName)) {
                    return db.get("name").asText();
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse json", e);
        }

        return null;
    }

    private String createDatabase(GrafanaClient client, String database) {
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
            return database;
        } else {
            log.trace("Database {} already exists and is called {}!", database, databaseName);
            return databaseName;
        }
    }
}
