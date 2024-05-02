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

package nl.tno.essim.observation.consumers.csv;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CSVObservationConsumer implements IObservationConsumer {

	private String csvFilesLocation;
	private HashMap<String, Table> tables;
	private ExecutorService pool;

	public CSVObservationConsumer(String csvFilesLocation) {
		this.csvFilesLocation = csvFilesLocation;
		tables = new HashMap<String, Table>();
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public void init(String database) {

	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		String observationSource = observation.getTags().get("assetId");
		String subFolder = source.getProviderName();
		if (observationSource == null) {
			observationSource = source.getProviderName();
		}
		Table table = tables.get(observationSource);
		if (table == null) {
			table = new Table(Paths.get(csvFilesLocation, subFolder).toString(), observationSource);
			tables.put(observationSource, table);
		}
		pool.submit(new TableProcessor(table, simulationRunName, observation.getObservedAt(), observation.getValues(),
				observation.getTags()));
	}

	@Override
	public void cleanup() {
		for (Table table : tables.values()) {
			try {
				table.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		log.debug("Wrote {} CSV files..", tables.size());

	}

	@Override
	public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {

	}
}

