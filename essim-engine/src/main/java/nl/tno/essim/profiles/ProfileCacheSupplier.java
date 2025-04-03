package nl.tno.essim.profiles;

import esdl.AbstractDataConfiguration;
import esdl.DatabaseConfiguration;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

public interface ProfileCacheSupplier {
    String createUri(AbstractDataConfiguration configuration);
    TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName) throws Exception;

}
