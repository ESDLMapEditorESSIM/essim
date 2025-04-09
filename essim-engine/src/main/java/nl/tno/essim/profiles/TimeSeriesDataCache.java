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

package nl.tno.essim.profiles;

import esdl.EsdlFactory;
import esdl.ProfileElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.DateRangeUtils;
import nl.tno.essim.time.EssimTime;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class TimeSeriesDataCache {

    private static final int TIMESTAMP = 0;
    private static final int DATA = 1;

    @Getter
    private final String name;
    private final LocalDateTime[] timestamps;
    private final double[] values;
    private final int totalSize;
    @Getter
    private long lastAccessed;

    public TimeSeriesDataCache(String name, ResultSet resultSet, double annualChange) throws SQLException {
        this.name = name;

        // Move to the end of the ResultSet to get total size
        resultSet.last();
        totalSize = resultSet.getRow();
        log.debug("Caching {} values for timeseries {}", totalSize, name);
        resultSet.beforeFirst(); // Reset cursor to the beginning

        timestamps = new LocalDateTime[totalSize];
        values = new double[totalSize];

        int index = 0;

        while (resultSet.next()) {
            OffsetDateTime timestamp = resultSet.getObject("time", OffsetDateTime.class);
            timestamps[index] = timestamp.toLocalDateTime();

            double value = resultSet.getDouble("value");
            values[index] = value * (1 + (annualChange / 100));

            index++;
        }

        lastAccessed = System.currentTimeMillis();

    }

    public TimeSeriesDataCache(String name, List<List<Object>> seriesData, double annualChange) {
        this.name = name;
        int size = seriesData.size();
        totalSize = size;
        timestamps = new LocalDateTime[totalSize];
        values = new double[totalSize];

        // validate datatypes
        if (!seriesData.isEmpty()) {
            List<Object> mainListFirstDataPoint = seriesData.getFirst();
            Object object = mainListFirstDataPoint.get(DATA);
            if (!(object instanceof Number)) {
                throw new IllegalArgumentException("Error reading timeseries data with name=" + name + ", data is not a numeric datatype, but is of type " + object.getClass().getSimpleName());
            }
        }

        for (int i = 0; i < size; i++) {
            List<Object> list = seriesData.get(i);
            timestamps[i] = LocalDateTime.parse((String) list.get(TIMESTAMP), DateTimeFormatter.ISO_DATE_TIME);
            values[i] = ((double) list.get(DATA)) * (1 + (annualChange / 100));
        }

        lastAccessed = System.currentTimeMillis();
    }

    public EList<ProfileElement> get(LocalDateTime start, LocalDateTime end, DataProcessor dataProcessor) {
        lastAccessed = System.currentTimeMillis();
        EList<ProfileElement> item = ECollections.newBasicEList();

        for (int i = 0; i < totalSize - 1; i++) {
            if (DateRangeUtils.hasOverlap(start, end, timestamps[i], timestamps[i + 1])) {
                ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                profileElement.setFrom(EssimTime.localDateTimeToDate(timestamps[i]));
                profileElement.setTo(EssimTime.localDateTimeToDate(timestamps[i + 1]));
                profileElement.setValue(dataProcessor.process(values[i]));
                item.add(profileElement);
            }
            if (!end.isAfter(timestamps[i + 1])) {
                break;
            }
        }
        return item;
    }

}
