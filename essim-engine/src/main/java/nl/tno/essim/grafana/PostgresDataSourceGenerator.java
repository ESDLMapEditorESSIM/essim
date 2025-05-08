package nl.tno.essim.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.CredentialUtils;
import nl.tno.essim.observation.consumers.PostgresObservationConsumer;

import java.net.URI;

@Slf4j
public class PostgresDataSourceGenerator implements DataSourceGenerator {
    private final static String DS_HANDLE = "DS_ESSIM_PG";
    private final String postgresUrl;
    private String databaseUid;

    public PostgresDataSourceGenerator(String postgresUrl) {
        this.postgresUrl = postgresUrl;
    }

    @Override
    public String getDBName(String resourceName) {
        return PostgresObservationConsumer.convertToValidPostgresColumnName(resourceName);
    }

    @Override
    public ObjectNode createDataSource(GrafanaClient grafanaClient, String databaseName) {
        ObjectNode dsInput = ESSimEngine.getObjectMapper().createObjectNode();
        try {
            if (databaseUid == null) {
                databaseUid = createDatabase(grafanaClient, databaseName);
            }
            dsInput.put("name", databaseName);
            dsInput.put("uid", databaseUid);
            dsInput.put("description", "");
            dsInput.put("type", "datasource");
            dsInput.put("pluginId", "postgres");
            dsInput.put("pluginName", "PostgreSQL");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return dsInput;
    }

    @Override
    public void addDataSourceReference(ObjectNode node) {
        ObjectNode dsNode = ESSimEngine.getObjectMapper().createObjectNode();
        dsNode.put("uid", databaseUid);
        dsNode.put("type", "postgres");
        node.set("datasource", dsNode);
    }

    @Override
    public String getJsonPath() {
        return "/json/postgres/";
    }


    private String checkDatabase(GrafanaClient client, String databaseName) {
        try {
            String datasourcesString = client.get(DATASOURCES_URL);
            JsonNode databases = ESSimEngine.getObjectMapper().readTree(datasourcesString);
            for (Object dbObj : databases) {
                ObjectNode db = (ObjectNode) dbObj;
                String dbName = db.get("database").asText();

                if (dbName.contains(databaseName)) {
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
            CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(postgresUrl);
            String username = credentials == null ? "essim" : credentials.username();
            String password = credentials == null ? "pgpassword" : credentials.password();

            URI uri = URI.create(postgresUrl);
            String grafanaUrl = uri.getHost();
            int port = uri.getPort();
            if (port > 0) {
                grafanaUrl += ":" + port;
            }
//            "jsonData": {
//                "postgresVersion": 903,
//                        "sslmode": "disable",
//                        "tlsAuth": false,
//                        "tlsAuthWithCACert": false,
//                        "tlsConfigurationMethod": "file-path",
//                        "tlsSkipVerify": true
//            },


            dbJSON.put("name", database);
            dbJSON.put("database", uri.getPath().substring(1));
            dbJSON.put("type", "postgres");
            dbJSON.put("typeName", "PostgreSQL");
            dbJSON.put("access", "proxy");
            dbJSON.put("url", grafanaUrl);
            dbJSON.put("user", username);
//            dbJSON.put("password", password);
            dbJSON.put("basicAuth", false);
            ObjectNode jsonData = ESSimEngine.getObjectMapper().createObjectNode();
            jsonData.put("postgresVersion", 903);
            jsonData.put("sslmode", "disable");
            jsonData.put("tlsAuth", false);
            jsonData.put("tlsAuthWithCACert", false);
            jsonData.put("tlsConfigurationMethod", "file-path");
            jsonData.put("tlsSkipVerify", true);
            dbJSON.set("jsonData", jsonData);
            ObjectNode secureJsonData = ESSimEngine.getObjectMapper().createObjectNode();
            secureJsonData.put("password", password);
            dbJSON.set("secureJsonData", secureJsonData);
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
