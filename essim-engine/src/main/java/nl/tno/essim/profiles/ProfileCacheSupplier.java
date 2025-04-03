package nl.tno.essim.profiles;

import esdl.AbstractDataConfiguration;

import java.util.Date;

public interface ProfileCacheSupplier {
    String createUri(AbstractDataConfiguration configuration);
    TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, Date startDate, Date endDate) throws Exception;

}
