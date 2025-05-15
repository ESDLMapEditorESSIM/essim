package nl.tno.essim.profiles;

import esdl.Duration;
import esdl.InfluxDBProfile;
import esdl.ProfileElement;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.CredentialUtils;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.EList;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.QueryResult;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
public class InfluxDBProfileDataHandler implements ProfileDataHandler {
    private static final int MAX_ATTEMPTS = 1;
    private final InfluxDBProfile profile;
    private final DataProcessor dataProcessor;
    private TimeSeriesDataCache dataCache;

    public InfluxDBProfileDataHandler(InfluxDBProfile profile, EssimTime essimTime) {
        this.profile = profile;
        Date profileStartDate = profile.getStartDate();
        if (profile.getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(profile.getProfileQuantityAndUnit(), profile.getMultiplier());
        } else {
            dataProcessor = new DataProcessor(profile.getProfileType(), profile.getMultiplier());
        }
        if (profileStartDate != null) {
            // Make sure the cache is loaded
            getProfileElements(profileStartDate, Date.from(profileStartDate.toInstant().plusSeconds(essimTime.getSimulationStepLength().getSeconds())), Converter.toESDLDuration(essimTime.getSimulationStepLength()));
        }
    }

    private InfluxDB connectToInfluxHost() {
        if (profile.getHost().isEmpty() || profile.getPort() <= 0) {
            throw new IllegalArgumentException("Postgres profile not configured correctly: " + this);
        }
        final String serverId = profile.getHost() + ":" + profile.getPort();
        CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(serverId);
        return credentials == null ? InfluxDBFactory.connect(serverId).enableGzip() : InfluxDBFactory.connect(serverId, credentials.username(), credentials.password()).enableGzip();
    }

    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        if (dataCache == null) {
            ProfileCacheKey profileCacheKey = getProfileCacheKey();
            synchronized (ProfileCache.getInstance()) {
                dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
                if (dataCache == null) {
                    dataCache = createTimeSeriesDataCache(profileCacheKey);
                } else {
                    log.debug("Using profile {} from cache", profileCacheKey);
                }
            }
        }
        return dataCache.get(EssimTime.dateToLocalDateTime(from), EssimTime.dateToLocalDateTime(to), dataProcessor);
    }

    @Override
    public ProfileCacheKey getProfileCacheKey() {
        String uri = "http://" + profile.getHost() + ":" + profile.getPort();
        return new ProfileCacheKey(uri, profile.getDatabase(), profile.getMeasurement(), profile.getField(), 0d);
    }

    private TimeSeriesDataCache createTimeSeriesDataCache(ProfileCacheKey profileCacheKey) {
        log.debug("Profile {} is not cached, proceeding to query InfluxDB", profileCacheKey);

        try (InfluxDB influxClient = connectToInfluxHost()) {
            String command = buildQueryString();
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
                    throw new IllegalArgumentException("No results returned on querying " + command + " at database [" + profile.getDatabase() + "] at [" + profile.getHost() + ":" + profile.getPort() + "] for profile with id " + profile.getId() + ". Please verify the database name, measurement and field names and timeframe of query!");
                }
                for (QueryResult.Series series : result.getSeries()) {
                    log.debug("Query returned {} values for {}", series.getValues().size(), profileCacheKey);
                    dataCache = new TimeSeriesDataCache(profile.getField(), series.getValues(), 0d);
                }
            }
            ProfileCache.getInstance().cache(profileCacheKey, dataCache);
        } catch (InfluxDBIOException e) {
            throw new RuntimeException("Error querying InfluxDB: " + e.getMessage(), e);
        }
        return dataCache;
    }

    @NotNull
    private String buildQueryString() {
        Date profileStartDate = profile.getStartDate();
        Date profileEndDate = profile.getEndDate();
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

        String command = "SELECT \"" + profile.getField() + "\" FROM \"" + profile.getMeasurement() + "\"";
        boolean hasWhereClause = false;
        if (startDateString != null) {
            command += " WHERE time >= '" + startDateString + "' AND time <= '" + endDateString + "'";
            hasWhereClause = true;
        }

        if (profile.getField() != null && !profile.getFilters().trim().isEmpty()) {
            final String filter = profile.getFilters().trim();
            command += (hasWhereClause ? " AND " : " WHERE ") + (filter.startsWith("AND") ? filter.substring(3).trim() : filter);
        }
        return command;
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not implemented for InfluxdbProfile");
    }
}
