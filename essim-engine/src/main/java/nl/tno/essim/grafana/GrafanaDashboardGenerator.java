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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GrafanaDashboardGenerator {
    static final String GRAFANA_EXTERNAL_URL_ENV = "GRAFANA_EXTERNAL_URL";

    private static final String JSON_RESOURCES = "/json/influxdb/";

    private static final String GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE = "GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE";
    private static final String GRAFANA_TRANSPORT_SOLVER_PANEL_WATT = "GRAFANA_TRANSPORT_SOLVER_PANEL_WATT";
    private static final String GRAFANA_LOAD_PANEL_JOULE = "GRAFANA_LOAD_PANEL_JOULE";
    private static final String GRAFANA_LOAD_PANEL_WATT = "GRAFANA_LOAD_PANEL_WATT";
    private static final String GRAFANA_LOAD_PANEL_PERCENT = "GRAFANA_LOAD_PANEL_PERCENT";
    private static final String DASHBOARDS_URL = "/api/dashboards/db";
    private DataSourceGenerator dataSourceGenerator;
    private GrafanaClient grafanaClient;

    @Getter
    private String dashboardUrl;
    private final String simulationRunName;

    record PanelInfo(String panelJson,
                     boolean isTransportPanel,
                     String titlePostfix
    ) {
    }

    public GrafanaDashboardGenerator(String user, String timeString, String databaseURL, List<TransportSolver> solversList,
                                     String energySystemId, String scenarioName, String simulationRunName, LocalDateTime simStartDate,
                                     LocalDateTime simEndDate, boolean emissionRow, boolean haveLoadflow, IMongoBackend mongo) {
        this.simulationRunName = simulationRunName;

        if (databaseURL == null) {
            log.warn("No database Observation Consumer defined! Dashboard will not be created!");
            return;
        }

        if (System.getenv(GrafanaClient.GRAFANA_INTERNAL_URL_ENV) == null) {
            log.warn("No internal Grafana URL defined! Dashboard will not be created!");
            return;
        }
        grafanaClient = new GrafanaClient(mongo);
        ObjectNode dashboardJSON = createDashboardJson(user, timeString, energySystemId, simStartDate, simEndDate);
        dataSourceGenerator = DataSourceGenerator.getDataSourceGenerator(databaseURL);
        ObjectNode dataSource = dataSourceGenerator.createDataSource(grafanaClient, scenarioName);
        dashboardJSON.set("__inputs", ESSimEngine.getObjectMapper().createArrayNode().add(dataSource));

        ArrayList<PanelInfo> panels = createPanelList(haveLoadflow);

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
                    addSolverPanel(panelArray, panel.panelJson(), solverId, panel.titlePostfix, index);
                }
            }
        }

        ObjectNode makeDashboardJSON = ESSimEngine.getObjectMapper().createObjectNode();
        makeDashboardJSON.set("dashboard", dashboardJSON);
        makeDashboardJSON.put("folderId", 0);
        makeDashboardJSON.put("overwrite", true);

        log.debug("Grafana: POSTing to {} with content: \n{}", DASHBOARDS_URL, makeDashboardJSON.toString());

        String response = grafanaClient.post(DASHBOARDS_URL, makeDashboardJSON.toString());
        ObjectNode respJSON = null;

        try {
            respJSON = (ObjectNode) ESSimEngine.getObjectMapper().readTree(response);
            if (respJSON.has("status")) {
                if (respJSON.get("status").asText().equals("success")) {
                    log.debug("Dashboard successfully created!!");
                    String grafanaExternalUrl = System.getenv(GRAFANA_EXTERNAL_URL_ENV);

                    if (grafanaExternalUrl == null) {
                        grafanaExternalUrl = System.getenv(GrafanaClient.GRAFANA_INTERNAL_URL_ENV);
                        log.warn("No external Grafana URL defined! Going to use {} in dashboard URL!", grafanaExternalUrl);
                    }

                    dashboardUrl = grafanaExternalUrl + respJSON.get("url").asText();
                    log.debug("Go to {}", dashboardUrl);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ArrayList<PanelInfo> createPanelList(boolean haveLoadflow) {
        ArrayList<PanelInfo> panels = new ArrayList<>();
        if (showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE, true)) {
            panels.add(new PanelInfo("tp-solver-panel.json", false, " (in J)"));
        }
        if (showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_WATT, true)) {
            panels.add(new PanelInfo("tp-solver-panel-power.json", false, " (in W)"));
        }
        if (haveLoadflow && showPanel(GRAFANA_TRANSPORT_SOLVER_PANEL_WATT, true)) {
            panels.add(new PanelInfo("tp-solver-panel-loadflow-power.json", false, " (loadflow in W)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_JOULE, false)) {
            panels.add(new PanelInfo("transport-loads-joule.json", true, " Transport flows (in J)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_WATT, true)) {
            panels.add(new PanelInfo("transport-loads-watt.json", true, " Transport flows (in W)"));
        }
        if (haveLoadflow && showPanel(GRAFANA_LOAD_PANEL_WATT, true)) {
            panels.add(new PanelInfo("transport-losses-watt.json", true, " Transport losses (in W)"));
        }
        if (showPanel(GRAFANA_LOAD_PANEL_PERCENT, true)) {
            panels.add(new PanelInfo("transport-loads-percent.json", true, " Transport loads (in %)"));
        }
        return panels;
    }

    private static boolean showPanel(String envVar, boolean defaultValue) {
        String value = System.getenv(envVar);
        return value == null ? defaultValue : value.trim().equalsIgnoreCase("true");
    }

    private ObjectNode readJson(String jsonResource) {
        return readJson(jsonResource, new HashMap<>());
    }

    private ObjectNode readJson(String jsonResource, Map<String, String> replacements) {
        if (replacements == null) {
            replacements = new HashMap<>();
        }
        replacements.put("\\$\\$SimulationRunName\\$\\$", simulationRunName);
        replacements.put("/?\\^?\\$SimulationRun\\$/?", simulationRunName);
        String jsonString = null;
        try (InputStream stream = GrafanaDashboardGenerator.class.getResourceAsStream(JSON_RESOURCES + jsonResource)) {
            jsonString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                jsonString = jsonString.replaceAll(entry.getKey(), entry.getValue());
            }
            return (ObjectNode) ESSimEngine.getObjectMapper().readTree(jsonString);
        } catch (IOException e) {
            log.warn("Could not read or parse json resource {}", jsonResource, e);
            log.debug("JSON resource content: {}", jsonString);
        }
        return null;
    }

    private void addSolverPanel(ArrayNode panelArray, String solverPanelJson, String solverId, String titlePostfix, int index) {
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("\\${3}TRANSPORT-SOLVER-NAME\\${3}", solverId);
        ObjectNode solverPanelNode = readJson(solverPanelJson, replacements);
        solverPanelNode.put("title", solverId + titlePostfix);
        addDataSource(solverPanelNode);

        ObjectNode gridPos = (ObjectNode) solverPanelNode.get("gridPos");
        int solverPanelY = 25 + index * gridPos.get("h").asInt();
        int solverPanelId = 7 + index;

        gridPos.put("y", solverPanelY);
        solverPanelNode.put("id", solverPanelId);
        setSimulationRun(solverPanelNode, simulationRunName);
        solverPanelNode.path("targets").forEach(target -> {
            ((ObjectNode) target).put("measurement", solverId);
        });
        panelArray.add(solverPanelNode);
    }

    private void addNetworkBalanceTable(ArrayNode panelArray, String energySystemId) {
        ObjectNode netBalPanel = readJson("network-balance-table.json");
        addDataSource(netBalPanel);
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
        addDataSource(panelNode);
        setSimulationRun(panelNode, simulationRunName);
        panelArray.add(panelNode);
    }

    private void addDataSource(ObjectNode node) {
        dataSourceGenerator.addDataSourceReference(node);
    }

    private static void setSimulationRun(ObjectNode panelNode, String simulationRunName) {
    }

    private static ArrayNode addPanelArray(ObjectNode dashboardJSON) {
        ArrayNode panelArray = ESSimEngine.getObjectMapper().createArrayNode();
        dashboardJSON.set("panels", panelArray);
        return panelArray;
    }

}
