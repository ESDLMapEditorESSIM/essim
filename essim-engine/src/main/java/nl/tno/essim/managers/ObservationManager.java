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
package nl.tno.essim.managers;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationManager;
import nl.tno.essim.observation.IObservationProvider;
import org.slf4j.MarkerFactory;

import java.util.ArrayList;

@Slf4j
public class ObservationManager implements IObservationManager {

    private final ArrayList<IObservationConsumer> consumers = new ArrayList<>();
    private final String simulationRunName;

    public ObservationManager(String simulationRunName) {
        this.simulationRunName = simulationRunName;
    }

    @Override
    public void registerConsumer(IObservationConsumer consumer) {
        consumers.add(consumer);
    }

    @Override
    public void publish(IObservationProvider source, IObservation observation) {
        for (IObservationConsumer consumer : consumers) {
            consumer.consume(simulationRunName, source, observation);
        }
    }

    @Override
    public void cleanupConsumers() {
        log.debug(MarkerFactory.getMarker(simulationRunName), "Please wait while the observations are being processed. This could take a few seconds...");
        for (IObservationConsumer consumer : consumers) {
            consumer.cleanup();
        }
    }

}