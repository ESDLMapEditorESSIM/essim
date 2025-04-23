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

package nl.tno.essim.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.ITransportSolver;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.transportsolver.TransportSolver;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
public class GrafanaClient {

    private static final String JSON_RESOURCES = "/json/";

    private static final String GRAFANA_ADMIN_USER_ENV = "GRAFANA_ADMIN_USER";
    private static final String GRAFANA_ADMIN_PASSWORD_ENV = "GRAFANA_ADMIN_PASSWORD";
    private static final String GRAFANA_INTERNAL_URL_ENV = "GRAFANA_INTERNAL_URL";
    private static final String GRAFANA_EXTERNAL_URL_ENV = "GRAFANA_EXTERNAL_URL";
    private static final String GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE = "GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE";
    private static final String GRAFANA_TRANSPORT_SOLVER_PANEL_WATT = "GRAFANA_TRANSPORT_SOLVER_PANEL_WATT";
    private static final String GRAFANA_LOAD_PANEL_JOULE = "GRAFANA_LOAD_PANEL_JOULE";
    private static final String GRAFANA_LOAD_PANEL_WATT = "GRAFANA_LOAD_PANEL_WATT";
    private static final String GRAFANA_LOAD_PANEL_PERCENT = "GRAFANA_LOAD_PANEL_PERCENT";
    private static final String GRAFANA_KEY = "GRAFANA_KEY";
    private static final String DATASOURCES_URL = "/api/datasources";
    private static final String DASHBOARDS_URL = "/api/dashboards/db";
    private static final String AUTH_URL = "/api/auth/keys";
    private static final String DS_HANDLE = "DS_ESSIM";
    private String databaseLabel;

    private String grafanaAdminKey = "";
    private final String grafanaInternalUrl;
    @Getter
    private String dashboardUrl;
    private final String simulationRunName;
    private final IMongoBackend mongo;
    private String grafanaAdminAuthorisation;

    record PanelInfo(ObjectNode panel,
                     boolean isTransportPanel,
                     String titlePostfix
    ) {
    }

    public GrafanaClient(String user, String timeString, String influxDBURL, List<TransportSolver> solversList,
                         String energySystemId, String scenarioName, String simulationRunName, LocalDateTime simStartDate,
                         LocalDateTime simEndDate, boolean emissionRow, boolean haveLoadflow, IMongoBackend mongo) {
        this.simulationRunName = simulationRunName;
        this.mongo = mongo;

        grafanaInternalUrl = System.getenv(GRAFANA_INTERNAL_URL_ENV);
        String grafanaExternalUrl = System.getenv(GRAFANA_EXTERNAL_URL_ENV);

        if (grafanaInternalUrl == null) {
            log.warn("No internal Grafana URL defined! Dashboard will not be created!");
            return;
        }

        if (influxDBURL == null) {
            log.warn("No InfluxDB Observation Consumer defined! Dashboard will not be created!");
            return;
        }

        if (grafanaExternalUrl == null) {
            log.warn("No external Grafana URL defined! Going to use " + grafanaInternalUrl + " in dashboard URL!");
            grafanaExternalUrl = grafanaInternalUrl;
        }

        String grafanaAdminUser = System.getenv(GRAFANA_ADMIN_USER_ENV) == null ? "admin"
                : System.getenv(GRAFANA_ADMIN_USER_ENV);
        String grafanaAdminPassword = System.getenv(GRAFANA_ADMIN_PASSWORD_ENV) == null ? "admin"
                : System.getenv(GRAFANA_ADMIN_PASSWORD_ENV);
        grafanaAdminAuthorisation = Base64.getEncoder()
                .encodeToString((grafanaAdminUser + ":" + grafanaAdminPassword).getBytes());

        grafanaAdminKey = createGrafanaAdminKey();


        ArrayList<PanelInfo> panels = new ArrayList<>();
        if (showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE, true)) {
            panels.add(new PanelInfo(readJson("tp-solver-panel.json"), false, " (in J)"));
        }
        if (showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_WATT, true)) {
            panels.add(new PanelInfo(readJson("tp-solver-panel-power.json"), false, " (in W)"));
        }
        if (haveLoadflow && showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_WATT, true)) {
            panels.add(new PanelInfo(readJson("tp-solver-panel-loadflow-power.json"), false, " (loadflow in W)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_JOULE, false)) {
            panels.add(new PanelInfo(readJson("transport-loads-joule.json"), true, " Transport flows (in J)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_WATT, true)) {
            panels.add(new PanelInfo(readJson("transport-loads-watt.json"), true, " Transport flows (in W)"));
        }
        if (haveLoadflow && showPanel(GRAFANA_LOAD_PANEL_WATT, true)) {
            panels.add(new PanelInfo(readJson("transport-losses-watt.json"), true, " Transport losses (in W)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_PERCENT, true)) {
            panels.add(new PanelInfo(readJson("transport-loads-percent.json"), true, " Transport loads (in %)"));
        }

        ObjectNode dashboardJSON = createDashboardJson(user, timeString, energySystemId, simStartDate, simEndDate);
        databaseLabel = addDatasource(dashboardJSON, influxDBURL, scenarioName);
        ArrayNode panelArray = addPanelArray(dashboardJSON);
        addSimpelPanel(panelArray, readJson("total-emissions.json"));
        addSimpelPanel(panelArray, readJson("emission-per-carrier.json"));
        addSimpelPanel(panelArray, readJson("sim-desc-panel.json"));
        addNetworkBalanceTable(panelArray, energySystemId);
        if (emissionRow) {
            addSimpelPanel(panelArray, readJson("emission-row-prod.json"));
        }

        // Add solver panels
        int index = 0;
        for (ITransportSolver solver : solversList) {
            String solverId = solver.getId();
            for (PanelInfo panel : panels) {
                if (!panel.isTransportPanel || solver.hasAnyTransportAsset()) {
                    addSolverPanel(panelArray, panel.panel(), solverId, panel.titlePostfix, index);
                }
            }
        }

        ObjectNode makeDashboardJSON = ESSimEngine.getObjectMapper().createObjectNode();
        makeDashboardJSON.set("dashboard", dashboardJSON);
        makeDashboardJSON.put("folderId", 0);
        makeDashboardJSON.put("overwrite", true);

        log.debug("Grafana: POSTing to {} with content: \n{}", DASHBOARDS_URL, makeDashboardJSON.toString());

        String response = post(DASHBOARDS_URL, makeDashboardJSON.toString());
        ObjectNode respJSON = null;
        try {
            respJSON = (ObjectNode) ESSimEngine.getObjectMapper().readTree(response);
            if (respJSON.has("status")) {
                if (respJSON.get("status").asText().equals("success")) {
                    log.debug("Dashboard successfully created!!");
                    dashboardUrl = grafanaExternalUrl + respJSON.get("url").asText();
                    log.debug("Go to {}", dashboardUrl);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean showPanel(String envVar, boolean defaultValue) {
        String value = System.getenv(envVar);
        return value == null ? defaultValue : value.trim().equalsIgnoreCase("true");
    }

    private ObjectNode readJson(String jsonResource) {
        try (InputStream stream = GrafanaClient.class.getResourceAsStream(JSON_RESOURCES + jsonResource)){
            String string = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String replaced = string.replaceAll("\\$\\$SimulationRunName\\$\\$", simulationRunName).replaceAll("/?\\^?\\$SimulationRun\\$/?", simulationRunName);
            return (ObjectNode) ESSimEngine.getObjectMapper().readTree(replaced);
        } catch (IOException e) {
            log.warn("Could not read json resource {}", jsonResource, e);
        }
        return null;
    }

    private void addSolverPanel(ArrayNode panelArray, ObjectNode solverPanelNode, String solverId, String titlePostfix, int index) {
        solverPanelNode.put("title", solverId + titlePostfix);
        solverPanelNode.put("datasource", databaseLabel);

        ObjectNode gridPos = (ObjectNode) solverPanelNode.get("gridPos");
        int solverPanelY = 25 + index * gridPos.get("h").asInt();
        int solverPanelId = 7 + index;

        gridPos.put("y", solverPanelY);
        solverPanelNode.put("id", solverPanelId);
        setSimulationRun(solverPanelNode, simulationRunName);
        solverPanelNode.path("targets").forEach(target -> ((ObjectNode) target).put("measurement", solverId));
        panelArray.add(solverPanelNode);
    }

    private void addNetworkBalanceTable(ArrayNode panelArray, String energySystemId) {
        ObjectNode netBalPanel = readJson("network-balance-table.json");
        netBalPanel.put("datasource", databaseLabel);
        setSimulationRun(netBalPanel, simulationRunName);
        netBalPanel.path("targets").forEach(target -> {
            ((ObjectNode) target).put("measurement", "/" + energySystemId + "(.*)/");
        });
        panelArray.add(netBalPanel);
    }

    @NotNull
    private ObjectNode createDashboardJson(String user, String timeString, String energySystemId, LocalDateTime simStartDate, LocalDateTime simEndDate) {
        ObjectNode dashboardJSON = readJson("dashboard.json");
        dashboardJSON.set("id", NullNode.getInstance());
        dashboardJSON.set("uid", NullNode.getInstance());
        dashboardJSON.put("title", energySystemId + " [" + user + "] [" + timeString + "]");
        dashboardJSON.set("time",
                ESSimEngine.getObjectMapper().createObjectNode()
                        .put("from",
                                DateTimeFormatter.ISO_INSTANT
                                        .format(ZonedDateTime.of(simStartDate, ZoneId.systemDefault())))
                        .put("to", DateTimeFormatter.ISO_INSTANT
                                .format(ZonedDateTime.of(simEndDate, ZoneId.systemDefault()))));
        return dashboardJSON;
    }

    private void addSimpelPanel(ArrayNode panelArray, ObjectNode panelNode) {
        panelNode.put("datasource", databaseLabel);
        setSimulationRun(panelNode, simulationRunName);
        panelArray.add(panelNode);
    }

    private static void setSimulationRun(ObjectNode panelNode, String simulationRunName) {
    }

    private static ArrayNode addPanelArray(ObjectNode dashboardJSON) {
        ArrayNode panelArray = ESSimEngine.getObjectMapper().createArrayNode();
        dashboardJSON.set("panels", panelArray);
        return panelArray;
    }

    private String addDatasource(ObjectNode dashboardJSON, String influxDBURL, String scenarioName) {
        String databaseLabel = createDatabase(influxDBURL, scenarioName);
        ObjectNode dsInput = ESSimEngine.getObjectMapper().createObjectNode();
        dsInput.put("name", DS_HANDLE);
        dsInput.put("label", databaseLabel);
        dsInput.put("description", "");
        dsInput.put("type", "datasource");
        dsInput.put("pluginId", "influxdb");
        dsInput.put("pluginName", "InfluxDB");
        dashboardJSON.set("__inputs", ESSimEngine.getObjectMapper().createArrayNode().add(dsInput));
        return databaseLabel;
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

    private String checkDatabase(String influxDBURL, String databaseName) {
        try {
            String datasourcesString = get(DATASOURCES_URL);
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

    private String createDatabase(String influxDBURL, String database) {
        String databaseName = checkDatabase(influxDBURL, database);

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
            String response = post(DATASOURCES_URL, dbJSON.toString());
            log.debug("Response: {}", response);
            return database;
        } else {
            log.trace("Database {} already exists and is called {}!", database, databaseName);
            return databaseName;
        }
    }

    private String get(String path) {
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
                        + " failed. Please check if the service is up and running.");
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

    private String postAdmin(String path, String body) {
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

    private String post(String path, String body) {
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
}
