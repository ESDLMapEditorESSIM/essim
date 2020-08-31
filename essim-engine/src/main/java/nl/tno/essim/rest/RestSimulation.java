/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */
package nl.tno.essim.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.Simulation;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.model.CreatedStatusImpl;
import nl.tno.essim.model.ErrorStatusImpl;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.KPIModule;
import nl.tno.essim.model.KPIModuleInfo;
import nl.tno.essim.model.RemoteKPIModule;
import nl.tno.essim.model.Status;
import nl.tno.essim.model.TransportNetwork;
import nl.tno.essim.mongo.MongoBackend;

@Slf4j
public class RestSimulation implements Simulation {

	private MongoBackend mongo;

	private UncaughtExceptionHandler essimExceptionHandler = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Caught Exception in REST handler: " + e.getMessage());
		}
	};

	public RestSimulation() {
		mongo = MongoBackend.getInstance();
	}

	@Override
	public PostSimulationResponse postSimulation(EssimSimulation simulation) {
		ErrorStatusImpl error = new ErrorStatusImpl();
		error.setStatus(Status.ERROR);

		String esdlContents = simulation.getEsdlContents();
		FileWriter fw = null;
		File esdlFile = null;
		if (esdlContents == null || esdlContents.isEmpty()) {
			error.setDescription("Invalid ESDL file!");
			return PostSimulationResponse.respond400WithApplicationJson(error);
		} else {
			try {
				String decodedContents;
				try {
					decodedContents = new String(Base64.getDecoder().decode(esdlContents), "UTF-8");
				} catch (IllegalArgumentException e) {
					log.debug("ESDL content is not Base64 encoded. Trying URL decoder...");
					decodedContents = URLDecoder.decode(esdlContents,"UTF-8");
				}
				esdlFile = File.createTempFile("ess", ".esdl", null);
				fw = new FileWriter(esdlFile);
				fw.write(decodedContents);
				fw.close();
				log.debug("Created temp file: {}", esdlFile.getPath());
			} catch (IOException e) {
				error.setDescription("Temp file write error!");
				return PostSimulationResponse.respond400WithApplicationJson(error);
			}
		}

		try {
			String simId;
			if (mongo != null) {
				simId = mongo.addSimulation(simulation);

				log.debug("Simulation ID : {}", simId);

				Date now = new Date();
				simulation.setSimRunDate(now);
				ESSimEngine engine = new ESSimEngine(simId, simulation, esdlFile);
				String grafanaDashboard = engine.createGrafanaDashboard();
				if (grafanaDashboard != null) {
					simulation.setDashboardURL(grafanaDashboard);
				} else {
					simulation.setDashboardURL("*headless simulation*");
				}
				simulation.setTransport(engine.getNetworkDiags());

				mongo.updateSimulationData(simId, simulation);

				Thread thread = new Thread(() -> engine.startSimulation(), simId);
				thread.setUncaughtExceptionHandler(essimExceptionHandler);
				thread.start();

				mongo.getStatusMap().put(simId, engine);

				CreatedStatusImpl created = new CreatedStatusImpl();
				created.setStatus(Status.CREATED);
				created.setId(simId);

				return PostSimulationResponse.respond201WithApplicationJson(created);
			} else {
				error.setDescription("MongoDB internal error!");
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log.error(e.getMessage(), e);
			error.setDescription("Internal error: " + sw.toString());
		}

		return PostSimulationResponse.respond400WithApplicationJson(error);
	}

	@Override
	public GetSimulationBySimulationIdResponse getSimulationBySimulationId(String simulationId) {
		EssimSimulation simulation = mongo.getSimulation(simulationId);
		if (simulation == null) {
			ErrorStatusImpl error = new ErrorStatusImpl();
			error.setStatus(Status.ERROR);
			error.setDescription("SimulationID " + simulationId + " not found!");
			return GetSimulationBySimulationIdResponse.respond404WithApplicationJson(error);
		} else {
			return GetSimulationBySimulationIdResponse.respond200WithApplicationJson(simulation);
		}
	}

	@Override
	public GetSimulationTransportBySimulationIdResponse getSimulationTransportBySimulationId(String simulationId) {
		EssimSimulation simulation = mongo.getSimulation(simulationId);
		if (simulation != null) {
			return GetSimulationTransportBySimulationIdResponse
					.respond200WithApplicationJson(simulation.getTransport());
		} else {
			ErrorStatusImpl error = new ErrorStatusImpl();
			error.setStatus(Status.ERROR);
			error.setDescription("SimulationID " + simulationId + " not found!");
			return GetSimulationTransportBySimulationIdResponse.respond404WithApplicationJson(error);
		}
	}

	@Override
	public GetSimulationTransportBySimulationIdAndIndexResponse getSimulationTransportBySimulationIdAndIndex(
			String simulationId, String index) {
		EssimSimulation simulation = mongo.getSimulation(simulationId);
		if (simulation != null) {
			List<TransportNetwork> transportNetworks = simulation.getTransport();
			try {
				int i = Integer.parseInt(index);
				TransportNetwork transportNetwork = transportNetworks.get(i);
				return GetSimulationTransportBySimulationIdAndIndexResponse
						.respond200WithApplicationJson(transportNetwork);
			} catch (IndexOutOfBoundsException | NumberFormatException e) {
				ErrorStatusImpl error = new ErrorStatusImpl();
				error.setStatus(Status.ERROR);
				error.setDescription("Index " + index + " is not valid!");
				return GetSimulationTransportBySimulationIdAndIndexResponse.respond404WithApplicationJson(error);
			}
		} else {
			ErrorStatusImpl error = new ErrorStatusImpl();
			error.setStatus(Status.ERROR);
			error.setDescription("SimulationID " + simulationId + " not found!");
			return GetSimulationTransportBySimulationIdAndIndexResponse.respond404WithApplicationJson(error);
		}
	}

	@Override
	public GetSimulationStatusBySimulationIdResponse getSimulationStatusBySimulationId(String simulationId) {
		IStatusProvider engine = mongo.getStatusMap().get(simulationId);
		if (engine != null) {
			JSONObject status = new JSONObject();
			status.put("status", engine.getStatus());
			status.put("description", engine.getDescription());
			return GetSimulationStatusBySimulationIdResponse.respond200WithApplicationJson(status.toString());
		} else {
			ErrorStatusImpl error = new ErrorStatusImpl();
			error.setStatus(Status.ERROR);
			error.setDescription("SimulationID " + simulationId + " not found!");
			return GetSimulationStatusBySimulationIdResponse.respond404WithApplicationJson(error);
		}
	}

	@Override
	public PutSimulationBySimulationIdResponse putSimulationBySimulationId(String simulationId,
			EssimSimulation entity) {
		if (mongo.getSimulation(simulationId) == null) {
			ErrorStatusImpl error = new ErrorStatusImpl();
			error.setStatus(Status.ERROR);
			error.setDescription("SimulationID " + simulationId + " not found!");
			return PutSimulationBySimulationIdResponse.respond404WithApplicationJson(error);
		} else {
			mongo.updateSimulationData(simulationId, entity);
			return PutSimulationBySimulationIdResponse.respond202WithApplicationJson(simulationId);
		}
	}

	@Override
	public GetSimulationResponse getSimulation() {
		List<EssimSimulation> simulations = mongo.getSimulations();
		return GetSimulationResponse.respond200WithApplicationJson(simulations);
	}

	@Override
	public GetSimulationLoadAnimationBySimulationIdResponse getSimulationLoadAnimationBySimulationId(
			String simulationId) {
		// StringBuilder contentBuilder = new StringBuilder();
		// try (ZipInputStream zis = new ZipInputStream(new FileInputStream(simulationId
		// + ".zip"))) {
		// ZipEntry zipEntry = zis.getNextEntry();
		// byte[] buffer = new byte[1024];
		// int read = 0;
		// while (zipEntry != null) {
		// while ((read = zis.read(buffer)) > 0) {
		// contentBuilder.append(new String(buffer, 0, read));
		// }
		// zipEntry = zis.getNextEntry();
		// }
		// String loadAnimationJSON = contentBuilder.toString();
		// return
		// GetSimulationLoadAnimationBySimulationIdResponse.respond200WithApplicationJson(loadAnimationJSON);
		// } catch (Exception e) {
		// return GetSimulationLoadAnimationBySimulationIdResponse
		// .respond404WithApplicationJson("Animation not found! Error because " +
		// e.getMessage());
		// }
		return GetSimulationLoadAnimationBySimulationIdResponse
				.respond404WithApplicationJson("Feature temporarily disabled!");
	}

	@Override
	public GetSimulationKpiBySimulationIdResponse getSimulationKpiBySimulationId(String simulationId) {
		String error = "";
		EssimSimulation simulation = mongo.getSimulation(simulationId);
		if (simulation == null) {
			error = "SimulationID " + simulationId + " not found!";
		} else {
			RemoteKPIModule kpiModule = simulation.getKpiModule();
			if (kpiModule == null) {
				error = "SimulationID " + simulationId + " had no KPIs defined!";
			} else {
				if (kpiModule.getModules() == null || kpiModule.getModules().isEmpty()) {
					error = "SimulationID " + simulationId + " had no KPIs defined!";
				} else {
					JSONArray result = new JSONArray();
					for (KPIModule module : kpiModule.getModules()) {
						if (module.getId() != null) {
							JSONObject moduleJSON = new JSONObject();
							JSONObject moduleResult;
							String moduleResultString = module.getResult();
							if (moduleResultString != null) {
								try {
									moduleResult = new JSONObject(moduleResultString);
									// Calculation is complete, so display kpi as value
									if (!moduleResult.has("status")) {
										moduleResult.put("status", "Success");
									}
								} catch (JSONException e) {
									// Running calculation, so display progress as value
									if (Commons.isNumericString(moduleResultString)) {
										moduleResult = new JSONObject("{status: Calculating}");
									} else {
										moduleResult = new JSONObject("{status: Error}");
									}
									moduleResult.put("progress", moduleResultString);
								}
							} else {
								moduleResult = new JSONObject().put("status", "Not yet started");
							}
							moduleJSON.put(module.getId(), moduleResult);
							result.put(moduleJSON);
						}
					}
					return GetSimulationKpiBySimulationIdResponse.respond200WithApplicationJson(result.toString());
				}
			}
		}
		return GetSimulationKpiBySimulationIdResponse.respond404WithApplicationJson(error);
	}

	@Override
	public GetSimulationKpiBySimulationIdAndKpiIdResponse getSimulationKpiBySimulationIdAndKpiId(String simulationId,
			String kpiId) {
		ErrorStatusImpl error = new ErrorStatusImpl();
		error.setStatus(Status.ERROR);
		EssimSimulation simulation = mongo.getSimulation(simulationId);
		if (simulation == null) {
			error.setDescription("SimulationID " + simulationId + " not found!");
		} else {
			RemoteKPIModule kpiModule = simulation.getKpiModule();
			if (kpiModule == null) {
				error.setDescription("SimulationID " + simulationId + " had no KPIs defined!");
			} else {
				if (kpiModule.getModules() == null || kpiModule.getModules().isEmpty()) {
					error.setDescription("SimulationID " + simulationId + " had no KPIs defined!");
				} else {
					for (KPIModule module : kpiModule.getModules()) {
						if (module.getId() != null) {
							if (module.getId().equals(kpiId)) {
								JSONObject moduleResult;
								String moduleResultString = module.getResult();
								if (moduleResultString != null) {
									try {
										moduleResult = new JSONObject(moduleResultString);
										// Calculation is complete, so display kpi as value
										if (!moduleResult.has("status")) {
											moduleResult.put("status", "Success");
										}
									} catch (JSONException e) {
										// Running calculation, so display progress as value
										if (Commons.isNumericString(moduleResultString)) {
											moduleResult = new JSONObject("{status: Calculating}");
										} else {
											moduleResult = new JSONObject("{status: Error}");
										}
										moduleResult.put("progress", moduleResultString);
									}
								} else {
									moduleResult = new JSONObject().put("status", "Not yet started");
								}
								return GetSimulationKpiBySimulationIdAndKpiIdResponse
										.respond200WithApplicationJson(moduleResult.toString());
							}
						}
					}
					error.setDescription("KPI ID " + kpiId + " undefined for SimulationID " + simulationId + "!");
				}
			}
		}
		return GetSimulationKpiBySimulationIdAndKpiIdResponse.respond404WithApplicationJson(error);
	}

	@Override
	public GetSimulationKpiModulesResponse getSimulationKpiModules() {
		List<KPIModuleInfo> allKpiModules = mongo.getAllKpiModules();
		return GetSimulationKpiModulesResponse.respond200WithApplicationJson(allKpiModules);
	}

	@Override
	public GetSimulationStatusResponse getSimulationStatus() {
		// TODO Auto-generated method stub
		return null;
	}
}
