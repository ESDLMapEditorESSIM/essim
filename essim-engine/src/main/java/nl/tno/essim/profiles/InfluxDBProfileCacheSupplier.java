package nl.tno.essim.profiles;

import esdl.DatabaseConfiguration;

import java.time.ZonedDateTime;

public class InfluxDBProfileCacheSupplier implements ProfileCacheSupplier {
    @Override
    public String createUri(DatabaseConfiguration configuration) {
        return "";
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, ZonedDateTime startDate, ZonedDateTime endDate) {
        return null;
    }
}
