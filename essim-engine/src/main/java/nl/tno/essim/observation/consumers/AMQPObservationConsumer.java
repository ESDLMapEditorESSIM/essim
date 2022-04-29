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
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

@Slf4j
public class AMQPObservationConsumer implements IObservationConsumer {

	private static final String DEFAULT_AMQP_USER = "guest";
	private static final String DEFAULT_AMQP_PASS = "guest";
	private static final String OBSERVATION_TOPIC = "observation";
	private static final String EXCHANGE = "THE_EXCHANGE";
	private String observationTopic;
	private ExecutorService pool;
	private long timestamp;
	private Connection connection;
	private String amqpUser;
	private String amqpPass;
	private String amqpHost;
	private int amqpPort;
	private Channel channel;

	public AMQPObservationConsumer(String amqpURL) throws MalformedURLException {
		URL url = new URL(amqpURL);
		amqpHost = url.getHost();
		amqpPort = url.getPort();
		String amqpUser = System.getenv("AMQP_USER");
		String amqpPass = System.getenv("AMQP_PASS");

		this.amqpUser = (amqpUser == null ? DEFAULT_AMQP_USER : amqpUser);
		this.amqpPass = (amqpPass == null ? DEFAULT_AMQP_PASS : amqpPass);
	}

	@Override
	public void init(String scenarioName) {
		observationTopic = scenarioName + "." + OBSERVATION_TOPIC;
		pool = Executors.newWorkStealingPool();

		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(amqpHost);
			factory.setPort(amqpPort);
			factory.setUsername(amqpUser);
			factory.setPassword(amqpPass);
			factory.setAutomaticRecoveryEnabled(true);
			factory.setNetworkRecoveryInterval(2000);
			while (connection == null) {
				try {
					connection = factory.newConnection();
				} catch (ConnectException e) {
					log.error("Unable to connect to MQTT Server. Trying again in 2 seconds...");
					Thread.sleep(2000);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("Connected to RabbitMQ @ {}:{}", amqpHost, amqpPort);
		try {
			channel = connection.createChannel();
			channel.exchangeDeclare(EXCHANGE, "topic", false, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		pool.submit(new Runnable() {

			@Override
			public void run() {
				if (source.getProviderName().equals("ESSIM")) {
					return;
				}

				JSONObject point = new JSONObject();

				// Measurement & Time
				point.put("measurement", source.getProviderName());
				timestamp = observation.getObservedAt().atZone(EssimTime.defaultTimeZone).toEpochSecond();
				point.put("time", timestamp);

				// Fields
				JSONObject fieldSet = new JSONObject();
				for (Entry<String, Object> field : observation.getValues().entrySet()) {
					fieldSet.put(field.getKey(), field.getValue());
				}
				point.put("fields", fieldSet);

				// Tags
				JSONObject tagSet = new JSONObject();
				for (Entry<String, String> tag : observation.getTags().entrySet()) {
					tagSet.put(tag.getKey(), tag.getValue());
				}
				tagSet.put("simulationRun", simulationRunName);
				point.put("tags", tagSet);

				if (channel != null) {
					try {
						channel.basicPublish(observationTopic, "", null, point.toString().getBytes());
					} catch (IOException e) {
						e.printStackTrace();
					}
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
			channel.basicPublish(observationTopic, "", null, endOfSimulation.toString().getBytes());
			channel.waitForConfirms();
			channel.close();
		} catch (IOException | InterruptedException | TimeoutException e) {
			e.printStackTrace();
		}

	}

}
