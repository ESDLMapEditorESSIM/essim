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
package nl.tno.essim.observation.consumers;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import io.nats.client.Connection;
import io.nats.client.Nats;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

public class NATSObservationConsumer implements IObservationConsumer {

	private static final String OBSERVATION_TOPIC = "observation";
	private long timestamp;
	private ExecutorService pool;
	private String observationTopic;
	private Connection nc;
	private String url;
	
	public NATSObservationConsumer(String url) {
		this.url = url;
	}

	@Override
	public void init(String scenarioName) {
		observationTopic = scenarioName + "." + OBSERVATION_TOPIC;
		pool = Executors.newWorkStealingPool();

		try {
			nc = Nats.connect(url);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				if (source.getProviderName()
						.equals("ESSIM")) {
					return;
				}

				JSONObject point = new JSONObject();

				// Measurement & Time
				point.put("measurement", source.getProviderName());
				timestamp = observation.getObservedAt()
						.atZone(EssimTime.defaultTimeZone)
						.toEpochSecond();
				point.put("time", timestamp);

				// Fields
				JSONObject fieldSet = new JSONObject();
				for (Entry<String, Object> field : observation.getValues()
						.entrySet()) {
					fieldSet.put(field.getKey(), field.getValue());
				}
				point.put("fields", fieldSet);

				// Tags
				JSONObject tagSet = new JSONObject();
				for (Entry<String, String> tag : observation.getTags()
						.entrySet()) {
					tagSet.put(tag.getKey(), tag.getValue());
				}
				tagSet.put("simulationRun", simulationRunName);
				point.put("tags", tagSet);

				if (nc != null) {
					nc.publish(observationTopic, point.toString()
							.getBytes());
				}
			}
		});
	}

	@Override
	public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanup() {
		JSONObject endOfSimulation = new JSONObject();
		endOfSimulation.put("event", "endOfSimulation");
		endOfSimulation.put("lastTimestamp", timestamp);
		try {
			pool.shutdown();
			pool.awaitTermination(1, TimeUnit.HOURS);
			nc.publish(observationTopic, endOfSimulation.toString()
					.getBytes());
			nc.flush(null);
			nc.close();
		} catch (InterruptedException | TimeoutException e) {
			e.printStackTrace();
		}

	}

}
