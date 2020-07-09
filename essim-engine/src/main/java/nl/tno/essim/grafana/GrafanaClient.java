/*
 * 
 */
package nl.tno.essim.grafana;

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

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.ITransportSolver;
import nl.tno.essim.mongo.MongoBackend;
import nl.tno.essim.transportsolver.TransportSolver;

@Slf4j
public class GrafanaClient {

	private static final String JSON_RESOURCES = "json/";
	private static final String GRAFANA_DASHBOARD_JSON = JSON_RESOURCES + "dashboard.json";
	private static final String GRAFANA_TOTAL_EMISSIONS_PANEL = JSON_RESOURCES + "total-emissions.json";
	private static final String GRAFANA_EMISSIONS_PER_CARRIER_PANEL = JSON_RESOURCES + "emission-per-carrier.json";
	private static final String GRAFANA_SIM_DESC_PANEL = JSON_RESOURCES + "sim-desc-panel.json";
	private static final String GRAFANA_NETWORK_BALANCE_TABLE = JSON_RESOURCES + "network-balance-table.json";
	private static final String GRAFANA_PROD_EMISSIONS_PANEL = JSON_RESOURCES + "emission-row-prod.json";
	private static final String GRAFANA_CONS_EMISSIONS_PANEL = JSON_RESOURCES + "emission-row-cons.json";
	private static final String GRAFANA_SOLVER_PANEL = JSON_RESOURCES + "tp-solver-panel.json";
	private static final String GRAFANA_JLOAD_PANEL = JSON_RESOURCES + "transport-loads-joule.json";
	private static final String GRAFANA_PLOAD_PANEL = JSON_RESOURCES + "transport-loads-percent.json";

	private static final String GRAFANA_ADMIN_USER_ENV = "GRAFANA_ADMIN_USER";
	private static final String GRAFANA_ADMIN_PASSWORD_ENV = "GRAFANA_ADMIN_PASSWORD";
	private static final String GRAFANA_KEY = "GRAFANA_KEY";
	private static final String DATASOURCES_URL = "/api/datasources";
	private static final String DASHBOARDS_URL = "/api/dashboards/db";
	private static final String AUTH_URL = "/api/auth/keys";
	private static final String DS_HANDLE = "DS_AMELANDESSIM";

	private String grafanaAdminKey = "";
	private String grafanaUrl;
	@Getter
	private String dashboardUrl;
	private MongoBackend mongo;
	private String grafanaAdminUser;
	private String grafanaAdminPassword;
	private String grafanaAdminAuthorisation;

	public GrafanaClient(String user, String timeString, String grafanaUrl, String influxDBURL,
			List<TransportSolver> solversList, String energySystemId, String scenarioName, String simulationRunName,
			LocalDateTime simStartDate, LocalDateTime simEndDate) {
		this.grafanaUrl = grafanaUrl;

		grafanaAdminUser = System.getenv(GRAFANA_ADMIN_USER_ENV) == null ? "admin"
				: System.getenv(GRAFANA_ADMIN_USER_ENV);
		grafanaAdminPassword = System.getenv(GRAFANA_ADMIN_PASSWORD_ENV) == null ? "admin"
				: System.getenv(GRAFANA_ADMIN_PASSWORD_ENV);
		grafanaAdminAuthorisation = Base64.getEncoder()
				.encodeToString((grafanaAdminUser + ":" + grafanaAdminPassword).getBytes());

		mongo = MongoBackend.getInstance();
		grafanaAdminKey = createGrafanaAdminKey();

		String dashboardString = null;
		String totalEmissionsString = null;
		String emissionsPerCarrierString = null;
		String simDescPanelString = null;
		String networkBalanceTableString = null;
		String producerEmissionPanelString = null;
		String consumerEmissionPanelString = null;
		String tpSolverPanelString = null;
		String tpSolverJLoadPanelString = null;
		String tpSolverPLoadPanelString = null;
		try {
			dashboardString = Commons.readFileIntoString(GRAFANA_DASHBOARD_JSON);
			totalEmissionsString = Commons.readFileIntoString(GRAFANA_TOTAL_EMISSIONS_PANEL);
			emissionsPerCarrierString = Commons.readFileIntoString(GRAFANA_EMISSIONS_PER_CARRIER_PANEL);
			simDescPanelString = Commons.readFileIntoString(GRAFANA_SIM_DESC_PANEL);
			networkBalanceTableString = Commons.readFileIntoString(GRAFANA_NETWORK_BALANCE_TABLE);
			producerEmissionPanelString = Commons.readFileIntoString(GRAFANA_PROD_EMISSIONS_PANEL);
			consumerEmissionPanelString = Commons.readFileIntoString(GRAFANA_CONS_EMISSIONS_PANEL);
			tpSolverPanelString = Commons.readFileIntoString(GRAFANA_SOLVER_PANEL);
			tpSolverJLoadPanelString = Commons.readFileIntoString(GRAFANA_JLOAD_PANEL);
			tpSolverPLoadPanelString = Commons.readFileIntoString(GRAFANA_PLOAD_PANEL);
		} catch (IOException e) {
			return;
		}

		String databaseLabel = createDatabase(influxDBURL, scenarioName);

		JSONObject dsInput = new JSONObject();
		dsInput.put("name", DS_HANDLE);
		dsInput.put("label", databaseLabel);
		dsInput.put("description", "");
		dsInput.put("type", "datasource");
		dsInput.put("pluginId", "influxdb");
		dsInput.put("pluginName", "InfluxDB");

		JSONArray panelArray = new JSONArray();

		// 1
		totalEmissionsString = totalEmissionsString.replace("$$SimulationRunName$$", simulationRunName);
		JSONObject totalEmissionsPanel = new JSONObject(totalEmissionsString);
		if (totalEmissionsPanel.has("datasource")) {
			totalEmissionsPanel.put("datasource", databaseLabel);
		}
		panelArray.put(totalEmissionsPanel);

		// 2
		emissionsPerCarrierString = emissionsPerCarrierString.replace("$$SimulationRunName$$", simulationRunName)
				.replace("$$DBName$$", scenarioName).replace("$$InfluxDBURL$$", influxDBURL)
				.replace("$$ESID$$", energySystemId);
		JSONObject emissionsPerCarrierPanel = new JSONObject(emissionsPerCarrierString);
		if (emissionsPerCarrierPanel.has("datasource")) {
			emissionsPerCarrierPanel.put("datasource", databaseLabel);
		}
		panelArray.put(emissionsPerCarrierPanel);

		// 3
		JSONObject simDescPanel = new JSONObject(simDescPanelString);
		simDescPanel.put("datasource", databaseLabel);
		for (Object targetObj : simDescPanel.getJSONArray("targets")) {
			JSONObject target = (JSONObject) targetObj;
			for (Object tagObj : target.getJSONArray("tags")) {
				JSONObject tag = (JSONObject) tagObj;
				if (tag.has("key")) {
					if (tag.getString("key").equals("simulationRun")) {
						tag.put("value", simulationRunName);
					}
				}
			}
		}
		panelArray.put(simDescPanel);

		// 4
		JSONObject netBalPanel = new JSONObject(networkBalanceTableString);
		netBalPanel.put("datasource", databaseLabel);
		for (Object targetObj : netBalPanel.getJSONArray("targets")) {
			JSONObject target = (JSONObject) targetObj;
			target.put("measurement", "/" + energySystemId + "(.*)/");
			for (Object tagObj : target.getJSONArray("tags")) {
				JSONObject tag = (JSONObject) tagObj;
				if (tag.has("key")) {
					if (tag.getString("key").equals("simulationRun")) {
						tag.put("value", simulationRunName);
					}
				}
			}
		}
		panelArray.put(netBalPanel);

		// 5
		producerEmissionPanelString = producerEmissionPanelString.replace("$$SimulationRunName$$", simulationRunName)
				.replace("$$DBName$$", scenarioName).replace("$$InfluxDBURL$$", influxDBURL)
				.replace("$$ESID$$", energySystemId);
		JSONObject prodEmissionsPanel = new JSONObject(producerEmissionPanelString);
		if (prodEmissionsPanel.has("datasource")) {
			prodEmissionsPanel.put("datasource", databaseLabel);
		}
		panelArray.put(prodEmissionsPanel);

		// 6
		consumerEmissionPanelString = consumerEmissionPanelString.replace("$$SimulationRunName$$", simulationRunName)
				.replace("$$DBName$$", scenarioName).replace("$$InfluxDBURL$$", influxDBURL);
		JSONObject consEmissionsPanel = new JSONObject(consumerEmissionPanelString);
		if (consEmissionsPanel.has("datasource")) {
			consEmissionsPanel.put("datasource", databaseLabel);
		}
		panelArray.put(consEmissionsPanel);

		int i = 0;
		for (ITransportSolver solver : solversList) {
			String solverId = solver.getId();

			JSONObject solverPanel = new JSONObject(tpSolverPanelString);
			solverPanel.put("title", solverId);
			solverPanel.put("datasource", databaseLabel);

			JSONObject gridPos = solverPanel.getJSONObject("gridPos");
			int solverPanelY = 25 + i * gridPos.getInt("h");
			int solverPanelId = 7 + i;

			gridPos.put("y", solverPanelY);
			solverPanel.put("id", solverPanelId);

			for (Object targetObj : solverPanel.getJSONArray("targets")) {
				JSONObject target = (JSONObject) targetObj;
				target.put("measurement", solverId);
				for (Object tagObj : target.getJSONArray("tags")) {
					JSONObject tag = (JSONObject) tagObj;
					if (tag.has("key")) {
						if (tag.getString("key").equals("simulationRun")) {
							tag.put("value", simulationRunName);
						}
					}
				}
			}

			panelArray.put(solverPanel);
			i++;

			if (solver.hasAnyTransportAsset()) {

				JSONObject jloadPanel = new JSONObject(tpSolverJLoadPanelString);
				jloadPanel.put("title", solverId + " Transport loads (in J)");
				jloadPanel.put("datasource", databaseLabel);

				gridPos = jloadPanel.getJSONObject("gridPos");
				int jloadPanelY = 25 + i * gridPos.getInt("h");
				int jloadPanelId = 7 + i;

				gridPos.put("y", jloadPanelY);
				jloadPanel.put("id", jloadPanelId);

				for (Object targetObj : jloadPanel.getJSONArray("targets")) {
					JSONObject target = (JSONObject) targetObj;
					target.put("measurement", solverId);
					for (Object tagObj : target.getJSONArray("tags")) {
						JSONObject tag = (JSONObject) tagObj;
						if (tag.has("key")) {
							if (tag.getString("key").equals("simulationRun")) {
								tag.put("value", simulationRunName);
							}
						}
					}
				}

				panelArray.put(jloadPanel);
				i++;

				JSONObject ploadPanel = new JSONObject(tpSolverPLoadPanelString);
				ploadPanel.put("title", solverId + " Transport loads (in percentage)");
				ploadPanel.put("datasource", databaseLabel);

				gridPos = ploadPanel.getJSONObject("gridPos");
				int ploadPanelY = 25 + i * gridPos.getInt("h");
				int ploadPanelId = 7 + i;

				gridPos.put("y", ploadPanelY);
				ploadPanel.put("id", ploadPanelId);

				for (Object targetObj : ploadPanel.getJSONArray("targets")) {
					JSONObject target = (JSONObject) targetObj;
					target.put("measurement", solverId);
					for (Object tagObj : target.getJSONArray("tags")) {
						JSONObject tag = (JSONObject) tagObj;
						if (tag.has("key")) {
							if (tag.getString("key").equals("simulationRun")) {
								tag.put("value", simulationRunName);
							}
						}
					}
				}

				panelArray.put(ploadPanel);
				i++;
			}
		}

		JSONObject dashboardJSON = new JSONObject(dashboardString);
		dashboardJSON.put("id", JSONObject.NULL);
		dashboardJSON.put("uid", JSONObject.NULL);
		dashboardJSON.put("title", energySystemId + " [" + user + "] [" + timeString + "]");
		dashboardJSON.put("__inputs", new JSONArray().put(dsInput));
		dashboardJSON.put("panels", panelArray);
		dashboardJSON.put("time",
				new JSONObject()
						.put("from",
								DateTimeFormatter.ISO_INSTANT
										.format(ZonedDateTime.of(simStartDate, ZoneId.systemDefault())))
						.put("to", DateTimeFormatter.ISO_INSTANT
								.format(ZonedDateTime.of(simEndDate, ZoneId.systemDefault()))));

		JSONObject makeDashboardJSON = new JSONObject();
		makeDashboardJSON.put("dashboard", dashboardJSON);
		makeDashboardJSON.put("folderId", 0);
		makeDashboardJSON.put("overwrite", true);

		String response = post(DASHBOARDS_URL, makeDashboardJSON.toString());
		JSONObject respJSON = new JSONObject(response);
		if (respJSON.has("status")) {
			if (respJSON.get("status").equals("success")) {
				log.debug("Dashboard successfully created!!");
				dashboardUrl = grafanaUrl + respJSON.get("url");
				log.debug("Go to {}", dashboardUrl);
			}
		}

	}

	private String createGrafanaAdminKey() {
		String key = mongo.getMetaInfo(GRAFANA_KEY);
		if (key != null) {
			log.debug("Found Grafana API Key: " + key);
		} else {
			log.debug("No Grafana API key found! Going to create one");
			String body = new JSONObject().put("name", String.valueOf(new Date().getTime())).put("role", "Admin")
					.toString();
			String result = postAdmin(AUTH_URL, body);
			log.trace("Result from querying Grafana : " + result);

			JSONObject adminResponse = new JSONObject(result);
			key = adminResponse.getString("key");

			mongo.updateMetaInfo(GRAFANA_KEY, key);
		}
		return key;
	}

	private String checkDatabase(String influxDBURL, String databaseName) {
		String datasourcesString = get(DATASOURCES_URL);
		JSONArray databases = new JSONArray(datasourcesString);
		for (Object dbObj : databases) {
			JSONObject db = (JSONObject) dbObj;
			String dbName = db.getString("database");

			if (dbName.contains(databaseName)) {
				return db.getString("name");
			}
		}

		return null;
	}

	private String createDatabase(String influxDBURL, String database) {
		String databaseName = checkDatabase(influxDBURL, database);

		if (databaseName == null) {
			JSONObject dbJSON = new JSONObject();
			dbJSON.put("name", database);
			dbJSON.put("database", database);
			dbJSON.put("type", "influxdb");
			dbJSON.put("url", influxDBURL);
			dbJSON.put("access", "proxy");
			dbJSON.put("jsonData", new JSONObject().put("keepCookies", new JSONArray()));
			dbJSON.put("secureJsonFields", new JSONArray());

			String response = post(DATASOURCES_URL, dbJSON.toString());
			log.debug(response);
			return database;
		} else {
			log.trace("Database {} already exists and is called {}!", database, databaseName);
			return databaseName;
		}
	}

	private String get(String path) {
		StringBuilder response = new StringBuilder();
		try {
			URL url = new URL(grafanaUrl + path);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Bearer " + grafanaAdminKey);

			BufferedReader br;
			try {
				log.trace("HTTP code for GET call to {}{} : {}", grafanaUrl, path, conn.getResponseCode());
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
			e.printStackTrace();
		}

		return response.toString();
	}

	private String postAdmin(String path, String body) {
		StringBuilder response = new StringBuilder();
		try {
			URL url = new URL(grafanaUrl + path);
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
			e.printStackTrace();
		}

		return response.toString();
	}

	private String post(String path, String body) {
		StringBuilder response = new StringBuilder();
		try {
			URL url = new URL(grafanaUrl + path);
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
				log.trace("HTTP code for POST call to {}{} with {} : {}", grafanaUrl, path, body,
						conn.getResponseCode());
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
			e.printStackTrace();
		}

		return response.toString();
	}
}
