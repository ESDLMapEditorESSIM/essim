package essim.impl;

import common.ProfileCacheKey;
import common.TimeSeriesDataCache;
import esdl.DatabaseConfiguration;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

public interface ProfileCacheSupplier {
    String createUri(DatabaseConfiguration configuration);
    TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, ZonedDateTime startDate, ZonedDateTime endDate) throws ExecutionException, SQLException;

}
