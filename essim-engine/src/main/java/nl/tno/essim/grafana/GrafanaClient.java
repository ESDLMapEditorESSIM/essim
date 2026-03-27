package nl.tno.essim.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.mongo.IMongoBackend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Date;

@Slf4j
public class GrafanaClient {
    public static final String GRAFANA_INTERNAL_URL_ENV = "GRAFANA_INTERNAL_URL";
    private static final String GRAFANA_ADMIN_USER_ENV = "GRAFANA_ADMIN_USER";
    private static final String GRAFANA_ADMIN_PASSWORD_ENV = "GRAFANA_ADMIN_PASSWORD";
    private static final String GRAFANA_KEY = "GRAFANA_KEY";
    private static final String AUTH_URL = "/api/auth/keys";


    private String grafanaAdminKey = "";
    private String grafanaAdminAuthorisation;
    private final String grafanaInternalUrl  = System.getenv(GRAFANA_INTERNAL_URL_ENV);
    private final IMongoBackend mongo;

    public GrafanaClient(IMongoBackend mongo) {
        this.mongo = mongo;
        String grafanaAdminUser = System.getenv(GRAFANA_ADMIN_USER_ENV) == null ? "admin"
                : System.getenv(GRAFANA_ADMIN_USER_ENV);
        String grafanaAdminPassword = System.getenv(GRAFANA_ADMIN_PASSWORD_ENV) == null ? "admin"
                : System.getenv(GRAFANA_ADMIN_PASSWORD_ENV);
        grafanaAdminAuthorisation = Base64.getEncoder()
                .encodeToString((grafanaAdminUser + ":" + grafanaAdminPassword).getBytes());
        grafanaAdminKey = createGrafanaAdminKey();
    }

    public String get(String path) {
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(grafanaInternalUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + grafanaAdminKey);

            BufferedReader br;
            try {
                log.trace("HTTP code for GET call to {}{} : {}", grafanaInternalUrl, path, conn.getResponseCode());
                br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            } catch (IOException e) {
                throw new IllegalArgumentException("Attempt to connect to Grafana at " + url.toString()
                        + " failed. Please check if the service is up and running.", e);
            }

            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            br.close();
            conn.disconnect();
        } catch (IOException e) {
            log.warn("Count not connect to grafana", e);
        }

        return response.toString();
    }

    public String postAdmin(String path, String body) {
        log.trace("Grafana admin posting:\n {}", body);
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(grafanaInternalUrl + path);
            log.trace("Going to do POST query : {}", url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + grafanaAdminAuthorisation);

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();

            BufferedReader br;
            try {
                log.trace("HTTP code for POST call to {} with {} : {}", url.toString(), body, conn.getResponseCode());
                br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            } catch (IOException e) {
                throw new IllegalArgumentException("Attempt to authenticate with Grafana at " + url.toString()
                        + " failed. Please check the admin credentials.");
            }

            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            br.close();
            conn.disconnect();
        } catch (IOException e) {
            log.warn("Could not post to Admin", e);
        }
        return response.toString();
    }

    public String post(String path, String body) {
        log.trace("Grafana posting:\n {}", body);
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(grafanaInternalUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + grafanaAdminKey);

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();

            BufferedReader br;
            try {
                log.trace("HTTP code for POST call to {}{} with {} : {}", grafanaInternalUrl, path, body,
                        conn.getResponseCode());
                br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            br.close();
            conn.disconnect();
        } catch (IOException e) {
            log.warn("Could not post to grafana", e);
        }

        return response.toString();
    }

    private String createGrafanaAdminKey() {
        String key = null;
        try {
            key = mongo.getMetaInfo(GRAFANA_KEY);
            if (key != null) {
                log.debug("Found Grafana API Key: {}", key);
            } else {
                log.debug("No Grafana API key found! Going to create one");
                String body = ESSimEngine.getObjectMapper().createObjectNode().put("name", String.valueOf(new Date().getTime())).put("role", "Admin")
                        .toString();
                String result = postAdmin(AUTH_URL, body);
                log.trace("Result from querying Grafana : {}", result);

                ObjectNode adminResponse = (ObjectNode) ESSimEngine.getObjectMapper().readTree(result);
                key = adminResponse.get("key").asText();

                mongo.updateMetaInfo(GRAFANA_KEY, key);
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse grafana response", e);
        }
        return key;
    }


}
