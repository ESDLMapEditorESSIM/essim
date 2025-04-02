package nl.tno.essim.profiles;

import esdl.Duration;
import esdl.InfluxDBProfile;
import esdl.ProfileElement;
import nl.tno.essim.commons.CredentialUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.EList;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.QueryResult;

import java.util.Date;

@Slf4j
public class InfluxdbProfileDataHandler implements ProfileDataHandler {
    private static final int MAX_ATTEMPTS = 1;
    private final InfluxDBProfile profile;
    private final DataProcessor dataProcessor;

    public InfluxdbProfileDataHandler(InfluxDBProfile profile) {
        this.profile = profile;
        if (profile.getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(profile.getProfileQuantityAndUnit(), profile.getMultiplier());
        } else {
            dataProcessor = new DataProcessor(profile.getProfileType(), profile.getMultiplier());
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
        String uri = "http://" + profile.getHost() + ":" + profile.getPort();
        ProfileCacheKey profileCacheKey = new ProfileCacheKey(uri, profile.getDatabase(), profile.getMeasurement(), profile.getField(), 0d);
        synchronized (ProfileCache.getInstance()) {
            TimeSeriesDataCache dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
            if (dataCache == null) {
                dataCache = createTimeSeriesDataCache(profileCacheKey, dataCache);
            } else {
                log.trace("Using profile {} from cache", profileCacheKey);
            }
            return dataCache.get(from.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                          to.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                          dataProcessor);
        }
    }

    private TimeSeriesDataCache createTimeSeriesDataCache(ProfileCacheKey profileCacheKey, TimeSeriesDataCache dataCache) {
        log.debug("Profile {} is not cached, proceeding to query InfluxDB", profileCacheKey);

        try (InfluxDB influxClient = connectToInfluxHost()) {
            String command = "SELECT \"" + profile.getField() + "\" FROM \"" + profile.getMeasurement() + "\"";

            if (profile.getField() != null && !profile.getFilters().trim().isEmpty()) {
                final String filter = profile.getFilters().trim();
                command += " WHERE " + (filter.startsWith("AND") ? filter.substring(3).trim() : filter);
            }
            log.debug("Influx Call for: {}", command);
            QueryResult queryResult = null;
            int connectionAttempts = 0;
            while (connectionAttempts < MAX_ATTEMPTS && queryResult == null) {
                connectionAttempts++;
                EssimQuery query = new EssimQuery(command, profile.getDatabase());
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
            log.error("Error querying InfluxDB: {}. Trying again...", e.getMessage());
        }
        return dataCache;
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not implemented for InfluxdbProfile");
    }
}
