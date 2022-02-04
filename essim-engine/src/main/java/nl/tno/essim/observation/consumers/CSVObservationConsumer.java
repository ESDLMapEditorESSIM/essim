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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;

@Slf4j
public class CSVObservationConsumer implements IObservationConsumer {

	private String csvFilesLocation;
	private HashMap<String, Table> tables;
	private ExecutorService pool;

	public CSVObservationConsumer(String csvFilesLocation) {
		this.csvFilesLocation = csvFilesLocation;
		tables = new HashMap<String, Table>();
		pool = Executors.newWorkStealingPool();
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

	@Override
	public void run() {
		try {
			synchronized (table) {
				table.addRow(simulationRunName, time, valueMap, tagMap);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class Table {

	@Getter
	private String name;
	private TreeSet<String> headers;
	private FileWriter fw;
	private boolean firstRow;
	private String delimiter;
	private String eolChar;
	private DateTimeFormatter timeFormat;

	public Table(String folderName, String name) {
		delimiter = System.getenv("CSV_DELIMITER");
		if (delimiter == null) {
			delimiter = ",";
		}
		eolChar = System.getenv("CSV_EOL_CHAR");
		if (eolChar == null) {
			eolChar = "\n";
		}
		String timeFormatString = System.getenv("CSV_TIME_FORMAT");
		if (timeFormatString == null) {
			timeFormatString = "yyyy-MM-dd HH:mm:ss";
		}
		timeFormat = DateTimeFormatter.ofPattern(timeFormatString);

		File file = new File(folderName);
		if (!file.exists()) {
			file.mkdirs();
		}
		this.name = name;
		headers = new TreeSet<String>();
		headers.add("Time");
		firstRow = true;
		try {
			fw = new FileWriter(Paths.get(folderName, name + ".csv").toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addRow(String simulationRunName, LocalDateTime time, Map<String, Object> valueMap,
			Map<String, String> tagMap) throws IOException {

		if (firstRow) {
			synchronized (fw) {
				headers.addAll(tagMap.keySet());
				headers.addAll(valueMap.keySet());
				fw.write(String.join(delimiter, headers.stream().collect(Collectors.toList())));
				fw.write(eolChar);
				firstRow = false;
			}
		}

		HashMap<String, Object> allMap = new HashMap<String, Object>();
		allMap.putAll(tagMap);
		allMap.putAll(valueMap);
		allMap.put("simulationRun", simulationRunName);
		synchronized (fw) {
			fw.write(time.atZone(ZoneOffset.UTC).format(timeFormat));
			fw.write(String.join(delimiter,
					headers.stream()
							.map(header -> allMap.get(header) == null ? ""
									: allMap.get(header).toString().contains(delimiter)
											? "\"" + allMap.get(header).toString() + "\""
											: allMap.get(header).toString())
							.collect(Collectors.toList())));
			fw.write(eolChar);
		}
	}

	public void close() throws IOException {
		fw.close();
	}
}
