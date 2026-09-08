package nl.tno.essim.profiles;

import esdl.AbstractDataConfiguration;
import esdl.DatabaseConfiguration;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.CredentialUtils;
import nl.tno.essim.time.EssimTime;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.QueryResult;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
public class InfluxDBProfileCacheSupplier implements ProfileCacheSupplier {
    private static final int MAX_ATTEMPTS = 1;
    private TimeSeriesDataCache dataCache;
    private DatabaseConfiguration dbConfig;

    @Override
    public String createUri(AbstractDataConfiguration configuration) {
        dbConfig = (DatabaseConfiguration) configuration;
        if (dbConfig.isTls()) {
        	return "https://" + dbConfig.getHost() + ":" + dbConfig.getPort();
        } else {
        	return "http://" + dbConfig.getHost() + ":" + dbConfig.getPort();
        }
    }

    private InfluxDB connectToInfluxHost(String serverUri) {
        CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(serverUri);
        return credentials == null ? InfluxDBFactory.connect(serverUri).enableGzip() : InfluxDBFactory.connect(serverUri, credentials.username(), credentials.password()).enableGzip();
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey profileCacheKey, String timeColumnName, Date startDate, Date endDate) {
        log.debug("Profile {} is not cached, proceeding to query InfluxDB", profileCacheKey);

        try (InfluxDB influxClient = connectToInfluxHost(profileCacheKey.uri())) {
            String command = buildQueryString(profileCacheKey, startDate, endDate);
            log.debug("Influx Call for: {}", command);
            QueryResult queryResult = null;
            int connectionAttempts = 0;
            while (connectionAttempts < MAX_ATTEMPTS && queryResult == null) {
                connectionAttempts++;
                EssimQuery query = new EssimQuery(command, profileCacheKey.databaseOrSchema());
                log.debug("InfluxDB query Attempt#{}: {}", connectionAttempts, query.getCommand());
                queryResult = influxClient.query(query);
                if (queryResult.hasError() || queryResult.getResults() == null) {
                    throw new IllegalStateException("Error querying InfluxDB for query " + query.getCommand() + ", queryError: " + queryResult.getError());
                }
            }
            for (QueryResult.Result result : queryResult.getResults()) {
                if (result.getSeries() == null) {
                    throw new IllegalArgumentException("No results returned on querying " + command + " at database [" + profileCacheKey.databaseOrSchema() + "] at [" + profileCacheKey.uri() + "]. Please verify the database name, measurement and field names and timeframe of query!");
                }
                for (QueryResult.Series series : result.getSeries()) {
                    log.debug("Query returned {} values for {}", series.getValues().size(), profileCacheKey);
                    dataCache = new TimeSeriesDataCache(profileCacheKey.field(), series.getValues(), 0d);
                }
            }
            ProfileCache.getInstance().cache(profileCacheKey, dataCache);
        } catch (InfluxDBIOException e) {
            log.error("Error querying InfluxDB: {}. Trying again...", e.getMessage());
        }
        return dataCache;
    }

    @NotNull
    private String buildQueryString(ProfileCacheKey cacheKey, Date profileStartDate, Date profileEndDate) {
        String startDateString = null;
        String endDateString = null;

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
            log.warn("Profile has no start date, querying everything");
        }

        return getCommand(cacheKey, startDateString, endDateString);
    }

    @NotNull
    private static String getCommand(ProfileCacheKey cacheKey, String startDateString, String endDateString) {
        String command = "SELECT \"" + cacheKey.field() + "\" FROM \"" + cacheKey.measurement() + "\"";
        if (startDateString != null) {
            command += " WHERE time >= '" + startDateString + "' AND time <= '" + endDateString + "'";
        }
        if (!(cacheKey.filters() == null || cacheKey.filters().trim().isEmpty())) {
            final String filter = cacheKey.filters().trim();
            boolean hasWhereClause = command.contains(" WHERE ");
            command += (hasWhereClause ? " AND " : " WHERE ") + (filter.startsWith("AND") ? filter.substring(3).trim() : filter);
        }
        return command;
    }


}
