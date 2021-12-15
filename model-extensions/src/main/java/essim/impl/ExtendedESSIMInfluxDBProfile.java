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

package essim.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;

import org.eclipse.emf.common.util.EList;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.json.JSONObject;
import org.json.JSONTokener;

import common.EssimQuery;
import common.ProfileCache;
import common.TimeSeriesDataCache;
import esdl.Duration;
import esdl.ProfileElement;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;

@Slf4j
public class ExtendedESSIMInfluxDBProfile extends ESSIMInfluxDBProfileImpl {

	private static final int MAX_ATTEMPTS = 1;
	private static final String USERNAME_PROP = "username";
	private static final String PASSWORD_PROP = "password";
	private InfluxDB influxClient;
	private TimeSeriesDataCache dataCache;
	private Duration aggregationPrecision;
	private ProfileCache profileCache;
	private LocalDateTime simStartDate;
	private LocalDateTime simEndDate;

	public ExtendedESSIMInfluxDBProfile() {
		super();
		profileCache = ProfileCache.getInstance();
	}

	private InfluxDB connectToInfluxHost() {
		final String serverId = getHost() + ":" + getPort();
		final File propFile = new File("credentials.json");

		log.debug("Looking for credentials in {}", propFile.getAbsolutePath());

		if (propFile.exists()) {
			try (InputStream is = new FileInputStream(propFile)) {
				JSONTokener parser = new JSONTokener(is);
				JSONObject root = new JSONObject(parser);
				if (root.has(serverId)) {
					JSONObject server = root.getJSONObject(serverId);

					if (server.has(USERNAME_PROP) && server.has(PASSWORD_PROP)) {
						log.debug("Using InfluxDB credentials read from config file {}.", propFile);
						return InfluxDBFactory
								.connect(serverId, server.getString(USERNAME_PROP), server.getString(PASSWORD_PROP))
								.enableGzip();
					} else {
						log.info(
								"Configuration for server found in {}, but missing fields {} and/or {}. Not using authentication",
								propFile, USERNAME_PROP, PASSWORD_PROP);
					}
				} else {
					log.info("Property file {} found, but no credential info for this server, not using authentication",
							propFile);
					log.info("serverId : {}", serverId);
					log.info("Prop file contents: {}", root.toString());
				}
			} catch (FileNotFoundException e) {
				log.warn("Unable to load config {}: {}", propFile, e.getMessage());
			} catch (IOException e) {
				log.error("Exception while reading config {}: {}", propFile, e.getMessage());
			}
		}

		return InfluxDBFactory.connect(serverId).enableGzip();
	}

	@Override
	public void initProfile(Date from, Date to, Duration aggregationPrecision) {

		if (from == null && to == null && aggregationPrecision == null) {
			profileCache.giveUp();
			return;
		}

		this.aggregationPrecision = aggregationPrecision;
		simStartDate = EssimTime.dateToLocalDateTime(from);
		simEndDate = EssimTime.dateToLocalDateTime(to);

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
			fillCache(getProfileQuantityAndUnit(), startDateString, endDateString,
					Converter.toEssimDuration(aggregationPrecision), getMultiplier(), getAnnualChangePercentage());
		} else {
			fillCache(getProfileType(), startDateString, endDateString, Converter.toEssimDuration(aggregationPrecision),
					getMultiplier(), getAnnualChangePercentage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date,
	 * esdl.Duration)
	 */
	@Override
	public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {

		if (this.aggregationPrecision != null
				&& !this.aggregationPrecision.getDurationUnit().equals(aggregationPrecision.getDurationUnit())
				&& (this.aggregationPrecision.getValue() == aggregationPrecision.getValue())) {
			log.warn("Different aggregation precisions are not (yet) supported and ignored!");
		}
		LocalDateTime startTime = EssimTime.dateToLocalDateTime(from);
		LocalDateTime endTime = EssimTime.dateToLocalDateTime(to);

		EList<ProfileElement> eList = dataCache.get(startTime, endTime, aggregationPrecision);
		for (ProfileElement profileElement : eList) {
			profileElement.setValue(profileElement.getValue() * getMultiplier());
		}
		return eList;
	}

	public void fillCache(Object profileType, String startTimeOfDataset, String endTime,
			EssimDuration simulationStepLength, double multiplier, double annualChange) {

		int connectionAttempts = 0;

		String command = "SELECT \"" + field + "\" FROM \"" + measurement + "\" WHERE time >= '" + startTimeOfDataset
				+ "' AND time <= '" + endTime + "'";

		log.debug("Influx Call for: " + command);

		synchronized (profileCache) {
			if (!profileCache.isCached(command, profileType)) {
				log.debug("Profile is not cached! Proceeding to query InfluxDB!");

				influxClient = connectToInfluxHost();

				QueryResult queryResult = null;
				while (connectionAttempts < MAX_ATTEMPTS && queryResult == null) {
					try {
						connectionAttempts++;
						EssimQuery query = new EssimQuery(command, database);
						log.debug("InfluxDB query Attempt#{}: {}", connectionAttempts, query.getCommand());
						queryResult = influxClient.query(query);
						if (queryResult.hasError() || queryResult.getResults() == null) {
							throw new IllegalStateException("Error querying InfluxDB for query " + query.getCommand()
									+ ", queryError: " + queryResult.getError());
						}
					} catch (InfluxDBIOException e) {
						log.error("Error querying InfluxDB: {}. Trying again...", e.getMessage());
					}
				}
				if (queryResult == null) {
					throw new IllegalArgumentException("Cannot connect to InfluxDB service at [" + getHost() + ":"
							+ getPort() + "] to query profile with id " + getId() + ". Please verify the URL!");
				}
				for (Result result : queryResult.getResults()) {
					if (result.getSeries() == null) {
						throw new IllegalArgumentException("No results returned on querying " + command
								+ " at database [" + database + "] at [" + getHost() + ":" + getPort()
								+ "] for profile with id " + getId()
								+ ". Please verify the database name, measurement and field names and timeframe of query!");
					}
					for (Series series : result.getSeries()) {
						dataCache = new TimeSeriesDataCache(field, series.getValues(), simStartDate, simEndDate,
								simulationStepLength, multiplier, annualChange, profileType);
					}
				}
				profileCache.cache(command, profileType, dataCache);
			} else {
				log.debug("Profile is cached! Retrieving from cache!");
				dataCache = profileCache.getDataCache(command, profileType);
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
			log.warn(
					"Using {} duration is not supported by InfluxDB, so it is mapped to 30 days, better use days using the number of days in a specific month",
					stepLength);
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
