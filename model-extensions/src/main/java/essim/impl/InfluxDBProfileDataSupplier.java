package essim.impl;

import common.ProfileCacheKey;
import common.TimeSeriesDataCache;
import esdl.DatabaseConfiguration;

import java.time.ZonedDateTime;

public class InfluxDBProfileDataSupplier implements ProfileDataSupplier {
    @Override
    public String createUri(DatabaseConfiguration configuration) {
        return "";
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, ZonedDateTime startDate, ZonedDateTime endDate) {
        return null;
    }
}
