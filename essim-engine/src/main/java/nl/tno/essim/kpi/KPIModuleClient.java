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
package nl.tno.essim.kpi;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;

import io.nats.client.Connection;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.KPIModule;
import nl.tno.essim.model.RemoteKPIModule;

@Slf4j
public class KPIModuleClient {
	private final static String CONTROL_TOPIC = "KPIControl";
	private RemoteKPIModule kpiModules;
	private String simulationId;
	private List<String> topicsOfInterest;
	private ExecutorService processPool;
	private double messages;
	private String natsURL;
	protected Connection natsClient;

	public KPIModuleClient(String simulationId, EssimSimulation simulation, double messages) {
		this.simulationId = simulationId;
		this.kpiModules = simulation.getKpiModule();
		this.natsURL = simulation.getNatsURL() == null ? "nats://nats:4222" : simulation.getNatsURL();
		this.messages = messages;
		this.topicsOfInterest = new ArrayList<String>();
		this.processPool = Executors.newWorkStealingPool();

		processPool.submit(starter);
	}

	private Runnable starter = new Runnable() {

		@Override
		public void run() {
			log.debug("Starting KPI Module Client");
			URI natsURI = URI.create(natsURL);
			try {
				natsClient = Nats.connect("nats://" + natsURI.getHost() + ":" + natsURI.getPort());
			} catch (IOException | InterruptedException e1) {
				e1.printStackTrace();
			}
			
			// Process KPI calculator request from MapEditor
			// and create a JSON message for the KPIControl topic
			JSONArray calculatorModules = new JSONArray();
			for (KPIModule module : kpiModules.getModules()) {
				@SuppressWarnings("unchecked")
				Map<String, String> config = (Map<String, String>) module.getConfig();
				JSONObject calculatorModuleConfig = new JSONObject(config);
				JSONObject calculatorModule = new JSONObject();
				calculatorModule.put("id", module.getId());
				calculatorModule.put("config", calculatorModuleConfig);
				calculatorModules.put(calculatorModule);

				topicsOfInterest.add(module.getId() + "." + simulationId);
			}
			JSONObject controlMessage = new JSONObject();
			controlMessage.put("simulationId", simulationId);
			controlMessage.put("kpiCalculators", calculatorModules);
			controlMessage.put("noOfMessages", messages);

			// Publish message for KPIControl topic
			if (natsClient != null) {
				natsClient.publish(CONTROL_TOPIC, controlMessage.toString().getBytes());
			}

			// Producer has done its job. Flush buffer and shutdown gracefully.
			try {
				natsClient.flush(null);
				natsClient.close();
			} catch (TimeoutException | InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	};
}
