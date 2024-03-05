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

package nl.tno.essim.tests;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

@Slf4j
public class InMemoryObservationConsumer implements IObservationConsumer {

	protected static final String TIME_KEY = "Time";

	@Getter
	private HashMap<String, Table> assets;
	@Getter
	private HashMap<String, Table> networks;
	private ExecutorService pool;

	public InMemoryObservationConsumer() {
		log.debug("Starting InMemoryObservationConsumer");

		assets = new HashMap<String, Table>();
		networks = new HashMap<String, Table>();
		pool = Executors.newSingleThreadExecutor();
	}

	@Override
	public void init(String database) {

	}

	private Table createTableHeader(String observationSource, IObservation observation) {
		Table table = Table.create(observationSource);
		table.addColumns(DateTimeColumn.create(TIME_KEY));
		for (Entry<String, Object> value : observation.getValues().entrySet()) {
			if (value.getValue() instanceof Double) {
				table.addColumns(DoubleColumn.create(value.getKey()));
			} else if (value.getValue() instanceof Float) {
				table.addColumns(FloatColumn.create(value.getKey()));
			} else if (value.getValue() instanceof Integer) {
				table.addColumns(IntColumn.create(value.getKey()));
			} else if (value.getValue() instanceof String) {
				table.addColumns(StringColumn.create(value.getKey()));
			} else if (value.getValue() instanceof BidFunction) {
				table.addColumns(StringColumn.create(value.getKey()));
			}
		}
		for (Entry<String, String> value : observation.getTags().entrySet()) {
			table.addColumns(StringColumn.create(value.getKey()));
		}
		return table;
	}

	enum ObservationType {
		ASSET, NETWORK,
	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		String observationSource = observation.getTags().get("assetId");
		ObservationType type = observationSource == null ? ObservationType.NETWORK : ObservationType.ASSET;

		HashMap<String, Table> dataframe = null;
		switch (type) {
		case NETWORK:
			observationSource = source.getProviderName();
			dataframe = networks;
			break;
		case ASSET:
			dataframe = assets;
			break;

		}

		Table table = dataframe.get(observationSource);
		if (table == null) {
			// table = new Table(Paths.get(csvFilesLocation, subFolder).toString(),
			// observationSource);
			table = createTableHeader(observationSource, observation);
			dataframe.put(observationSource, table);
		}
		pool.submit(new TableProcessor(table, simulationRunName, observation.getObservedAt(), observation.getValues(),
				observation.getTags()));
	}

	@Override
	public void cleanup() {
//		for (Table table : tables.values()) {
//			table.clear();
//		}
//
//		log.debug("Cleaning up InMemoryObservationConsumer");
		try {
			pool.shutdown();
			pool.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {

	}
}

@Slf4j
class TableProcessor implements Runnable {

	private Table table;
	private String simulationRunName;
	private LocalDateTime time;
	private Map<String, Object> valueMap;
	private Map<String, String> tagMap;

	public TableProcessor(Table table, String simulationRunName, LocalDateTime time, Map<String, Object> valueMap,
			Map<String, String> tagMap) {
		this.table = table;
		this.simulationRunName = simulationRunName;
		this.time = time;
		this.valueMap = valueMap;
		this.tagMap = tagMap;

	}

	private void fillRow(Row row, String simulationRunName, LocalDateTime time, Map<String, Object> valueMap,
			Map<String, String> tagMap) {
		row.setDateTime(InMemoryObservationConsumer.TIME_KEY, time);
		for (Entry<String, Object> entry : valueMap.entrySet()) {
			if (entry.getValue() instanceof Double) {
				row.setDouble(entry.getKey(), (Double) entry.getValue());
			} else if (entry.getValue() instanceof Float) {
				row.setFloat(entry.getKey(), (Float) entry.getValue());
			} else if (entry.getValue() instanceof Integer) {
				row.setInt(entry.getKey(), (Integer) entry.getValue());
			} else if (entry.getValue() instanceof String) {
				row.setString(entry.getKey(), (String) entry.getValue());
			} else if (entry.getValue() instanceof BidFunction) {
				BidFunction bidFunction = (BidFunction) entry.getValue();
				row.setString(entry.getKey(), bidFunction.toJsonArray());
			} else {
				log.warn("Unknown value type to convert {}", entry);
			}
		}
		for (Entry<String, String> entry : tagMap.entrySet()) {
			row.setString(entry.getKey(), String.valueOf(entry.getValue()));
		}
	}

	@Override
	public void run() {
		synchronized (table) {
			Row row = table.appendRow();
			fillRow(row, simulationRunName, time, valueMap, tagMap);
		}
	}

}
