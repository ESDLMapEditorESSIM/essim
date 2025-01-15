/**
 *  This work is based on original code developed and copyrighted by TNO 2020.
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 * <p>
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
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
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.ITransportSolver;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.transportsolver.TransportSolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
public class GrafanaClient {

	private static final String JSON_RESOURCES = "json/";
	private static final String GRAFANA_DASHBOARD_JSON = JSON_RESOURCES + "dashboard.json";
	private static final String GRAFANA_TOTAL_EMISSIONS_PANEL = JSON_RESOURCES + "total-emissions.json";
	private static final String GRAFANA_EMISSIONS_PER_CARRIER_PANEL = JSON_RESOURCES + "emission-per-carrier.json";
	private static final String GRAFANA_SIM_DESC_PANEL = JSON_RESOURCES + "sim-desc-panel.json";
	private static final String GRAFANA_NETWORK_BALANCE_TABLE = JSON_RESOURCES + "network-balance-table.json";
	private static final String GRAFANA_PROD_EMISSIONS_PANEL = JSON_RESOURCES + "emission-row-prod.json";
//	private static final String GRAFANA_CONS_EMISSIONS_PANEL = JSON_RESOURCES + "emission-row-cons.json";
	private static final String GRAFANA_SOLVER_PANEL = JSON_RESOURCES + "tp-solver-panel.json";
	private static final String GRAFANA_SOLVER_PANEL_POWER = JSON_RESOURCES + "tp-solver-panel-power.json";
	private static final String GRAFANA_SOLVER_PANEL_LOADFLOW_POWER = JSON_RESOURCES + "tp-solver-panel-loadflow-power.json";
	private static final String GRAFANA_JLOAD_PANEL = JSON_RESOURCES + "transport-loads-joule.json";
	private static final String GRAFANA_WLOAD_PANEL = JSON_RESOURCES + "transport-loads-watt.json";
	private static final String GRAFANA_WLOSSES_PANEL = JSON_RESOURCES + "transport-losses-watt.json";
	private static final String GRAFANA_PLOAD_PANEL = JSON_RESOURCES + "transport-loads-percent.json";

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

	private String grafanaAdminKey = "";
    private final String grafanaInternalUrl;
	@Getter
	private String dashboardUrl;
    private final IMongoBackend mongo;
	private String grafanaAdminAuthorisation;

	public GrafanaClient(String user, String timeString, String influxDBURL, List<TransportSolver> solversList,
			String energySystemId, String scenarioName, String simulationRunName, LocalDateTime simStartDate,
			LocalDateTime simEndDate, boolean emissionRow, boolean haveLoadflow, IMongoBackend mongo) {
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

		//mongo = MongoBackend.getInstance();
		grafanaAdminKey = createGrafanaAdminKey();

		String tsPanelJoule = System.getenv(GRAFANA_TRANSPORT_SOLVER_PANEL_JOULE);
        boolean grafanaTransportSolverPanelJoule;
		if (tsPanelJoule == null) {
			grafanaTransportSolverPanelJoule = true;
        } else grafanaTransportSolverPanelJoule = tsPanelJoule.trim().equalsIgnoreCase("true");

		String tsPanelWatt = System.getenv(GRAFANA_TRANSPORT_SOLVER_PANEL_WATT);
        boolean grafanaTransportSolverPanelWatt;
		if (tsPanelWatt == null) {
			grafanaTransportSolverPanelWatt = true;
        } else grafanaTransportSolverPanelWatt = tsPanelWatt.trim().equalsIgnoreCase("true");

		String loadPanelJoule = System.getenv(GRAFANA_LOAD_PANEL_JOULE);
        boolean grafanaLoadPanelJoule;
		if (loadPanelJoule == null) {
			grafanaLoadPanelJoule = false;
        } else grafanaLoadPanelJoule = loadPanelJoule.trim().equalsIgnoreCase("true");

		String loadPanelWatt = System.getenv(GRAFANA_LOAD_PANEL_WATT);
        boolean grafanaLoadPanelWatt;
		if (loadPanelWatt == null) {
			grafanaLoadPanelWatt = true;
        } else grafanaLoadPanelWatt = loadPanelWatt.trim().equalsIgnoreCase("true");

		String loadPanelPercent = System.getenv(GRAFANA_LOAD_PANEL_PERCENT);
        boolean grafanaLoadPanelPercent;
		if (loadPanelPercent == null) {
			grafanaLoadPanelPercent = true;
        } else grafanaLoadPanelPercent = loadPanelPercent.trim().equalsIgnoreCase("true");

		String dashboardString = null;
		String totalEmissionsString = null;
		String emissionsPerCarrierString = null;
		String simDescPanelString = null;
		String networkBalanceTableString = null;
		String producerEmissionPanelString = null;
//		String consumerEmissionPanelString = null;
		String tpSolverPanelString = null;
		String tpSolverPanelPowerString = null;
		String tpSolverPanelLoadflowPowerString = null;
		String tpSolverJLoadPanelString = null;
		String tpSolverWLoadPanelString = null;
		String tpSolverWLossesPanelString;
		String tpSolverPLoadPanelString = null;
		try {
			dashboardString = Commons.readFileIntoString(GRAFANA_DASHBOARD_JSON);
			totalEmissionsString = Commons.readFileIntoString(GRAFANA_TOTAL_EMISSIONS_PANEL);
			emissionsPerCarrierString = Commons.readFileIntoString(GRAFANA_EMISSIONS_PER_CARRIER_PANEL);
			simDescPanelString = Commons.readFileIntoString(GRAFANA_SIM_DESC_PANEL);
			networkBalanceTableString = Commons.readFileIntoString(GRAFANA_NETWORK_BALANCE_TABLE);
			producerEmissionPanelString = Commons.readFileIntoString(GRAFANA_PROD_EMISSIONS_PANEL);
//			consumerEmissionPanelString = Commons.readFileIntoString(GRAFANA_CONS_EMISSIONS_PANEL);
			tpSolverPanelString = Commons.readFileIntoString(GRAFANA_SOLVER_PANEL);
			tpSolverPanelPowerString = Commons.readFileIntoString(GRAFANA_SOLVER_PANEL_POWER);
			tpSolverPanelLoadflowPowerString = Commons.readFileIntoString(GRAFANA_SOLVER_PANEL_LOADFLOW_POWER);
			tpSolverJLoadPanelString = Commons.readFileIntoString(GRAFANA_JLOAD_PANEL);
			tpSolverWLoadPanelString = Commons.readFileIntoString(GRAFANA_WLOAD_PANEL);
			tpSolverWLossesPanelString = Commons.readFileIntoString(GRAFANA_WLOSSES_PANEL);
			tpSolverPLoadPanelString = Commons.readFileIntoString(GRAFANA_PLOAD_PANEL);
		} catch (IOException e) {
			return;
		}

        try {
		String databaseLabel = createDatabase(influxDBURL, scenarioName);
            ObjectNode dsInput = ESSimEngine.getObjectMapper().createObjectNode();
		dsInput.put("name", DS_HANDLE);
		dsInput.put("label", databaseLabel);
		dsInput.put("description", "");
		dsInput.put("type", "datasource");
		dsInput.put("pluginId", "influxdb");
		dsInput.put("pluginName", "InfluxDB");

            ArrayNode panelArray = ESSimEngine.getObjectMapper().createArrayNode();

		// 1
		totalEmissionsString = totalEmissionsString.replace("$$SimulationRunName$$", simulationRunName);
            ObjectNode totalEmissionsPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(totalEmissionsString);
		if (totalEmissionsPanel.has("datasource")) {
			totalEmissionsPanel.put("datasource", databaseLabel);
		}
            panelArray.add(totalEmissionsPanel);

		// 2
		emissionsPerCarrierString = emissionsPerCarrierString.replace("$$SimulationRunName$$", simulationRunName)
				.replace("$$DBName$$", scenarioName).replace("$$InfluxDBURL$$", influxDBURL)
				.replace("$$ESID$$", energySystemId);
            ObjectNode emissionsPerCarrierPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(emissionsPerCarrierString);
		if (emissionsPerCarrierPanel.has("datasource")) {
			emissionsPerCarrierPanel.put("datasource", databaseLabel);
		}
            panelArray.add(emissionsPerCarrierPanel);

		// 3
            ObjectNode simDescPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(simDescPanelString);
		simDescPanel.put("datasource", databaseLabel);
            for (JsonNode target : simDescPanel.get("targets")) {
                for (JsonNode tag : target.get("tags")) {
				if (tag.has("key")) {
                        if (tag.get("key").asText().equals("simulationRun")) {
                            ((ObjectNode) tag).put("value", simulationRunName);
					}
				}
			}
		}
            panelArray.add(simDescPanel);

		// 4
            ObjectNode netBalPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(networkBalanceTableString);
		netBalPanel.put("datasource", databaseLabel);
            for (var targetObj : netBalPanel.get("targets")) {
                ObjectNode target = (ObjectNode) targetObj;
			target.put("measurement", "/" + energySystemId + "(.*)/");
                for (var tagObj : target.get("tags")) {
                    ObjectNode tag = (ObjectNode) tagObj;
				if (tag.has("key")) {
                        if (tag.get("key").asText().equals("simulationRun")) {
						tag.put("value", simulationRunName);
					}
				}
			}
		}
            panelArray.add(netBalPanel);

		if (emissionRow) {
			// 5
			producerEmissionPanelString = producerEmissionPanelString
					.replace("$$SimulationRunName$$", simulationRunName).replace("$$DBName$$", scenarioName)
					.replace("$$InfluxDBURL$$", influxDBURL).replace("$$ESID$$", energySystemId);
                ObjectNode prodEmissionsPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(producerEmissionPanelString);
			if (prodEmissionsPanel.has("datasource")) {
				prodEmissionsPanel.put("datasource", databaseLabel);
			}
                panelArray.add(prodEmissionsPanel);
		}

		int i = 0;
		for (ITransportSolver solver : solversList) {
			String solverId = solver.getId();

			if (grafanaTransportSolverPanelJoule) {
                    ObjectNode solverPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverPanelString);
				solverPanel.put("title", solverId + " (in J)");
			solverPanel.put("datasource", databaseLabel);

                    ObjectNode gridPos = (ObjectNode) solverPanel.get("gridPos");
                    int solverPanelY = 25 + i * gridPos.get("h").asInt();
			int solverPanelId = 7 + i;

			gridPos.put("y", solverPanelY);
			solverPanel.put("id", solverPanelId);

                    for (JsonNode targetObj : solverPanel.get("targets")) {
                        ObjectNode target = (ObjectNode) targetObj;
				target.put("measurement", solverId);
                        for (JsonNode tagObj : target.get("tags")) {
                            ObjectNode tag = (ObjectNode) tagObj;
					if (tag.has("key")) {
                                if (tag.get("key").asText().equals("simulationRun")) {
							tag.put("value", simulationRunName);
						}
					}
				}
			}

                    panelArray.add(solverPanel);
			i++;
			}

			if (grafanaTransportSolverPanelWatt) {
                    ObjectNode solverPanelW = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverPanelPowerString);
				solverPanelW.put("title", solverId + " (in W)");
				solverPanelW.put("datasource", databaseLabel);

    //                JSONObject gridPos = solverPanelW.getJSONObject("gridPos");
                    ObjectNode gridPos = (ObjectNode) solverPanelW.get("gridPos");
                    int solverPanelY = 25 + i * gridPos.get("h").asInt();
				int solverPanelId = 7 + i;

				gridPos.put("y", solverPanelY);
				solverPanelW.put("id", solverPanelId);

                    for (JsonNode targetObj : solverPanelW.get("targets")) {
                        ObjectNode target = (ObjectNode) targetObj;
					target.put("measurement", solverId);
                        for (Object tagObj : target.get("tags")) {
                            ObjectNode tag = (ObjectNode) tagObj;
						if (tag.has("key")) {
                                if (tag.get("key").asText().equals("simulationRun")) {
								tag.put("value", simulationRunName);
							}
						}
					}
				}

                    panelArray.add(solverPanelW);
				i++;
			}


			if (grafanaTransportSolverPanelWatt && haveLoadflow) {
				ObjectNode solverPanelLfW = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverPanelLoadflowPowerString);
				solverPanelLfW.put("title", solverId + " (loadflow in W)");
				solverPanelLfW.put("datasource", databaseLabel);
				ObjectNode gridPos = (ObjectNode) solverPanelLfW.get("gridPos");
				int solverPanelY = 25 + i * gridPos.get("h").asInt();
				int solverPanelId = 7 + i;

				gridPos.put("y", solverPanelY);
				solverPanelLfW.put("id", solverPanelId);

				for (JsonNode targetObj : solverPanelLfW.get("targets")) {
					ObjectNode target = (ObjectNode) targetObj;
					target.put("measurement", solverId);
					for (Object tagObj : target.get("tags")) {
						ObjectNode tag = (ObjectNode) tagObj;
						if (tag.has("key")) {
							if (tag.get("key").asText().equals("simulationRun")) {
								tag.put("value", simulationRunName);
							}
						}
					}
				}

				panelArray.add(solverPanelLfW);
				i++;
			}



			if (solver.hasAnyTransportAsset()) {

				if (grafanaLoadPanelJoule) {
                        ObjectNode jloadPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverJLoadPanelString);
				jloadPanel.put("title", solverId + " Transport flows (in J)");
				jloadPanel.put("datasource", databaseLabel);

                        ObjectNode gridPos = (ObjectNode) jloadPanel.get("gridPos");
                        int jloadPanelY = 25 + i * gridPos.get("h").asInt();
				int jloadPanelId = 7 + i;

				gridPos.put("y", jloadPanelY);
				jloadPanel.put("id", jloadPanelId);

                        for (JsonNode targetObj : jloadPanel.get("targets")) {
                            ObjectNode target = (ObjectNode) targetObj;
					target.put("measurement", solverId);
                            for (Object tagObj : target.get("tags")) {
                                ObjectNode tag = (ObjectNode) tagObj;
						if (tag.has("key")) {
                                    if (tag.get("key").asText().equals("simulationRun")) {
								tag.put("value", simulationRunName);
							}
						}
					}
				}

                        panelArray.add(jloadPanel);
				i++;
				}

				if (grafanaLoadPanelWatt) {
                        ObjectNode wloadPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverWLoadPanelString);
					wloadPanel.put("title", solverId + " Transport flows (in W)");
					wloadPanel.put("datasource", databaseLabel);

                        ObjectNode gridPos = (ObjectNode) wloadPanel.get("gridPos");
                        int jloadPanelY = 25 + i * gridPos.get("h").asInt();
					int jloadPanelId = 7 + i;

					gridPos.put("y", jloadPanelY);
					wloadPanel.put("id", jloadPanelId);

                        for (JsonNode targetObj : wloadPanel.get("targets")) {
                            ObjectNode target = (ObjectNode) targetObj;
						target.put("measurement", solverId);
                            for (Object tagObj : target.get("tags")) {
                                ObjectNode tag = (ObjectNode) tagObj;
							if (tag.has("key")) {
                                    if (tag.get("key").asText().equals("simulationRun")) {
									tag.put("value", simulationRunName);
								}
							}
						}
					}

                        panelArray.add(wloadPanel);
					i++;
				}

				if (grafanaLoadPanelWatt) {
					ObjectNode wloadPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverWLossesPanelString);
					wloadPanel.put("title", solverId + " Transport losses (in W)");
					wloadPanel.put("datasource", databaseLabel);

					ObjectNode gridPos = (ObjectNode) wloadPanel.get("gridPos");
					int jloadPanelY = 25 + i * gridPos.get("h").asInt();
					int jloadPanelId = 7 + i;

					gridPos.put("y", jloadPanelY);
					wloadPanel.put("id", jloadPanelId);

					for (JsonNode targetObj : wloadPanel.get("targets")) {
						ObjectNode target = (ObjectNode) targetObj;
						target.put("measurement", solverId);
						for (Object tagObj : target.get("tags")) {
							ObjectNode tag = (ObjectNode) tagObj;
							if (tag.has("key")) {
								if (tag.get("key").asText().equals("simulationRun")) {
									tag.put("value", simulationRunName);
								}
							}
						}
					}

					panelArray.add(wloadPanel);
					i++;
				}


				if (solver.hasAnyTransportAssetWithCapacity() && grafanaLoadPanelPercent) {

                        ObjectNode ploadPanel = (ObjectNode) ESSimEngine.getObjectMapper().readTree(tpSolverPLoadPanelString);
					ploadPanel.put("title", solverId + " Transport loads (in %)");
					ploadPanel.put("datasource", databaseLabel);

                        ObjectNode gridPos = (ObjectNode) ploadPanel.get("gridPos");
                        int ploadPanelY = 25 + i * gridPos.get("h").asInt();
					int ploadPanelId = 7 + i;

					gridPos.put("y", ploadPanelY);
					ploadPanel.put("id", ploadPanelId);

                        for (JsonNode targetObj : ploadPanel.get("targets")) {
                            ObjectNode target = (ObjectNode) targetObj;
						target.put("measurement", solverId);
                            for (Object tagObj : target.get("tags")) {
                                ObjectNode tag = (ObjectNode) tagObj;
							if (tag.has("key")) {
                                    if (tag.get("key").asText().equals("simulationRun")) {
									tag.put("value", simulationRunName);
								}
							}
						}
					}

                        panelArray.add(ploadPanel);
					i++;
				}
			}
		}

            ObjectNode dashboardJSON = (ObjectNode) ESSimEngine.getObjectMapper().readTree(dashboardString);
            dashboardJSON.set("id", NullNode.getInstance());
            dashboardJSON.set("uid", NullNode.getInstance());
		dashboardJSON.put("title", energySystemId + " [" + user + "] [" + timeString + "]");
            dashboardJSON.set("__inputs", ESSimEngine.getObjectMapper().createArrayNode().add(dsInput));
            dashboardJSON.set("panels", panelArray);
            dashboardJSON.set("time",
                    ESSimEngine.getObjectMapper().createObjectNode()
						.put("from",
								DateTimeFormatter.ISO_INSTANT
										.format(ZonedDateTime.of(simStartDate, ZoneId.systemDefault())))
						.put("to", DateTimeFormatter.ISO_INSTANT
								.format(ZonedDateTime.of(simEndDate, ZoneId.systemDefault()))));

            ObjectNode makeDashboardJSON = ESSimEngine.getObjectMapper().createObjectNode();
            makeDashboardJSON.set("dashboard", dashboardJSON);
		makeDashboardJSON.put("folderId", 0);
		makeDashboardJSON.put("overwrite", true);

            String response = post(DASHBOARDS_URL, makeDashboardJSON.toString());
            ObjectNode respJSON = (ObjectNode) ESSimEngine.getObjectMapper().readTree(response);
		if (respJSON.has("status")) {
                if (respJSON.get("status").asText().equals("success")) {
				log.debug("Dashboard successfully created!!");
				dashboardUrl = grafanaExternalUrl + respJSON.get("url").asText();
				log.debug("Go to {}", dashboardUrl);
			}
		}
        } catch (JsonProcessingException e) {
            log.warn("Could not parse json", e);
        }

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
			log.trace("Going to do POST query : " + url.toString());

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
//                throw new IllegalArgumentException("Attempt to connect to Grafana at " + url.toString()
//                        + " failed. Please check if the service is up and running.");
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
