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

package common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import esdl.Duration;
import esdl.EsdlFactory;
import esdl.ProfileElement;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;

public class TimeSeriesDataCache {

	private static final int TIMESTAMP = 0;
	private static final int DATA = 1;
	private static final int YEARS = 1;

	private String name;
	private LocalDateTime[] timestamps;
	private double[] values;
	private int totalSize;

	public TimeSeriesDataCache(String name, List<List<Object>> influxDBSeriesData, LocalDateTime startDate,
			LocalDateTime endDate, EssimDuration simulationStep, double annualChange) {
		this.name = name;
		int size = influxDBSeriesData.size();
		totalSize = size * YEARS;
		timestamps = new LocalDateTime[totalSize];
		values = new double[totalSize];

		for (int j = 0; j < YEARS; j++) {
			for (int i = 0; i < size; i++) {
				List<Object> list = influxDBSeriesData.get(i);
				timestamps[j * size + i] = LocalDateTime.parse((String) list.get(TIMESTAMP),
						DateTimeFormatter.ISO_DATE_TIME);
				values[j * size + i] = ((double) list.get(DATA)) * (1 + (annualChange / 100) * j);
			}
		}

	}

	public EList<ProfileElement> get(LocalDateTime start, LocalDateTime end, Duration aggregationPrecision,
			DataProcessor dataProcessor) {
		EList<ProfileElement> item = ECollections.newBasicEList();

		for (int i = 0; (i < totalSize - 1) && (timestamps[i].isBefore(end)); i++) {
			if (timestamps[i].isEqual(start) || timestamps[i].isAfter(start)) {
				ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
				profileElement.setFrom(EssimTime.localDateTimeToDate(timestamps[i]));
				profileElement.setTo(EssimTime.localDateTimeToDate(timestamps[i + 1]));
				profileElement.setValue(dataProcessor.process(values[i]));
				item.add(profileElement);
			}
		}

		return item;
	}

	public String getName() {
		return name;
	}
}
