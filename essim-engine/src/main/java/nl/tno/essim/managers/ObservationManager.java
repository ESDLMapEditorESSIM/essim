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
package nl.tno.essim.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;

public class ObservationManager implements IObservationManager {

	private List<IObservationConsumer> consumers;
	private String simulationRunName;
	private ExecutorService executorService;

	public ObservationManager(String simulationRunName) {
		this.simulationRunName = simulationRunName;
		consumers = Collections.synchronizedList(new ArrayList<IObservationConsumer>());
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public void registerConsumer(IObservationConsumer consumer) {
		synchronized (consumers) {
			consumers.add(consumer);
		}
	}

	@Override
	public void registerConsumers(List<IObservationConsumer> consumerList) {
		synchronized (consumers) {
			consumers.addAll(consumerList);
		}
	}

	@Override
	public synchronized void publish(IObservationProvider source, IObservation observation) {
		synchronized (consumers) {
			for (IObservationConsumer consumer : consumers) {
				consumer.consume(simulationRunName, source, observation);
			}
		}
	}

	@Override
	public void publishBatch(IObservationProvider source, IObservation observation) {
		synchronized (consumers) {
			for (IObservationConsumer consumer : consumers) {
				executorService.submit(() -> consumer.consumeBatch(simulationRunName, source, observation));
			}
		}
	}
}