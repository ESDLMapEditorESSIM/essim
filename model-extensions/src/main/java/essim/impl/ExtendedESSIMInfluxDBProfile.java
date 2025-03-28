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

package essim.impl;

import common.*;
import esdl.Duration;
import esdl.ProfileElement;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.EList;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.QueryResult;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
public class ExtendedESSIMInfluxDBProfile extends ESSIMInfluxDBProfileImpl {

    private static final int MAX_ATTEMPTS = 1;
    private TimeSeriesDataCache dataCache;
    private Duration aggregationPrecision;
    private DataProcessor dataProcessor;

    public ExtendedESSIMInfluxDBProfile() {
        super();
    }

    private InfluxDB connectToInfluxHost() {
        if (getHost().isEmpty() || getPort() <= 0) {
            throw new IllegalArgumentException("Postgres profile not configured correctly: " + this);
        }
        final String serverId = getHost() + ":" + getPort();
        ProfileTools.DBCredentials dbCredentials = ProfileTools.getDBCredentials(serverId);
        return dbCredentials == null ? InfluxDBFactory.connect(serverId).enableGzip() : InfluxDBFactory.connect(serverId, dbCredentials.username(), dbCredentials.password()).enableGzip();
    }

    @Override
    public void initProfile(Date from, Date to, Duration aggregationPrecision) {

        if (from == null && to == null && aggregationPrecision == null) {
            return;
        }

        this.aggregationPrecision = aggregationPrecision;

        Date profileStartDate = getStartDate();
        Date profileEndDate = getEndDate();
        String startDateString;
        String endDateString;

        if (profileStartDate != null) {
            LocalDateTime startDateFromGUI = EssimTime.dateToLocalDateTime(profileStartDate);
            LocalDateTime endDateFromGUI;
            if (profileEndDate != null) {
                endDateFromGUI = EssimTime.dateToLocalDateTime(profileEndDate);
            } else {
                endDateFromGUI = startDateFromGUI.plusYears(1L);
            }
            startDateString = EssimTime.toInfluxDBTimeString(startDateFromGUI);
            endDateString = EssimTime.toInfluxDBTimeString(endDateFromGUI);
        } else {
            startDateString = EssimTime.toInfluxDBTimeString(EssimTime.dateToLocalDateTime(from));
            endDateString = EssimTime.toInfluxDBTimeString(EssimTime.dateToLocalDateTime(to));
        }
        if (getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(getProfileQuantityAndUnit(), getMultiplier());
        } else {
            dataProcessor = new DataProcessor(getProfileType(), getMultiplier());
        }
        updateCache(startDateString, endDateString, Converter.toEssimDuration(aggregationPrecision), getAnnualChangePercentage());
    }

    /*
     * (non-Javadoc)
     *
     * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date,
     * esdl.Duration)
     */
    @Override
    public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {
        if (this.aggregationPrecision != null && !this.aggregationPrecision.getDurationUnit().equals(aggregationPrecision.getDurationUnit()) && (this.aggregationPrecision.getValue() == aggregationPrecision.getValue())) {
            log.warn("Different aggregation precisions are not (yet) supported and ignored!");
        }
        return dataCache.get(EssimTime.dateToLocalDateTime(from), EssimTime.dateToLocalDateTime(to), dataProcessor);
    }

    public void updateCache(String startTimeOfDataset, String endTime, EssimDuration simulationStepLength, double annualChange) {

        int connectionAttempts = 0;

        String command = "SELECT \"" + field + "\" FROM \"" + measurement + "\" WHERE time >= '" + startTimeOfDataset + "' AND time <= '" + endTime + "'";

        if (this.filters != null && !this.filters.trim().isEmpty()) {
            final String filter = this.filters.trim();
            command += filter.startsWith("AND") ? " " + filter : " AND " + filter;
        }

        log.debug("Influx Call for: {}", command);
        String uri = "http://" + getHost() + ":" + getPort();
        ProfileCacheKey profileCacheKey = new ProfileCacheKey(uri, getDatabase(), getMeasurement(), getField(), getAnnualChangePercentage());

        synchronized (ProfileCache.getInstance()) {
            dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
            if (dataCache == null) {
                log.debug("Profile {} is not cached, proceeding to query InfluxDB", profileCacheKey);

                try (InfluxDB influxClient = connectToInfluxHost()) {

                    QueryResult queryResult = null;
                    while (connectionAttempts < MAX_ATTEMPTS && queryResult == null) {
                        connectionAttempts++;
                        EssimQuery query = new EssimQuery(command, database);
                        log.debug("InfluxDB query Attempt#{}: {}", connectionAttempts, query.getCommand());
                        queryResult = influxClient.query(query);
                        if (queryResult.hasError() || queryResult.getResults() == null) {
                            throw new IllegalStateException("Error querying InfluxDB for query " + query.getCommand() + ", queryError: " + queryResult.getError());
                        }
                    }
                    for (QueryResult.Result result : queryResult.getResults()) {
                        if (result.getSeries() == null) {
                            throw new IllegalArgumentException("No results returned on querying " + command + " at database [" + database + "] at [" + getHost() + ":" + getPort() + "] for profile with id " + getId() + ". Please verify the database name, measurement and field names and timeframe of query!");
                        }
                        for (QueryResult.Series series : result.getSeries()) {
                            log.debug("Query returned {} values for {}", series.getValues().size(), profileCacheKey);
                            dataCache = new TimeSeriesDataCache(field, series.getValues(), annualChange);
                        }
                    }
                    ProfileCache.getInstance().cache(profileCacheKey, dataCache);
                } catch (InfluxDBIOException e) {
                    log.error("Error querying InfluxDB: {}. Trying again...", e.getMessage());
                }
            } else {
                log.debug("Using profile {} from cache", profileCacheKey);
            }
        }
    }

    public String toInfluxDBTime(EssimDuration stepLength) {
        switch (stepLength.getUnit()) {
            case DAYS:
                return stepLength.getAmount() + "d";
            case HOURS:
                return stepLength.getAmount() + "h";
            case MICROS:
                return stepLength.getAmount() + "u";
            case MILLIS:
                return stepLength.getAmount() + "ms";
            case MINUTES:
                return stepLength.getAmount() + "m";
            case MONTHS:
                log.warn("Using {} duration is not supported by InfluxDB, so it is mapped to 30 days, better use days using the number of days in a specific month", stepLength);
                return 30 * stepLength.getAmount() + "d";
            case NANOS:
                return stepLength.getAmount() + "ns";
            case SECONDS:
                return stepLength.getAmount() + "s";
            case WEEKS:
                return 7 * stepLength.getAmount() + "d";
            case YEARS:
                return 365 * stepLength.getAmount() + "d";
            default:
                throw new UnsupportedOperationException(stepLength.getUnit().name() + " is not supported by ESSIM yet!");

        }
    }

}
