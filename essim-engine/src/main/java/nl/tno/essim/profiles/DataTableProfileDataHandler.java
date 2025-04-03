package nl.tno.essim.profiles;

import esdl.DataTableProfile;
import esdl.Duration;
import esdl.ProfileElement;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.Date;

@Slf4j
public class DataTableProfileDataHandler implements ProfileDataHandler {
    private final DataTableProfile profile;
    private final DataProcessor dataProcessor;
    private final ProfileCacheSupplier cacheSupplier;
    private TimeSeriesDataCache dataCache;

    public DataTableProfileDataHandler(DataTableProfile profile, EssimTime essimTime) {
        this.profile = profile;
        Date profileStartDate = profile.getStartDate();
        if (profile.getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(profile.getProfileQuantityAndUnit(), profile.getMultiplier());
        } else {
            dataProcessor = new DataProcessor(profile.getProfileType(), profile.getMultiplier());
        }
        cacheSupplier = ProfileCacheSupplierFactory.getProfileCacheSupplier(profile.getConfiguration());
        // Make sure the cache is loaded
        getProfileElements(profileStartDate, Date.from(profileStartDate.toInstant().plusSeconds(essimTime.getSimulationStepLength().getSeconds())), Converter.toESDLDuration(essimTime.getSimulationStepLength()));
    }

    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        if (dataCache == null) {
            String uri = cacheSupplier.createUri(profile.getConfiguration());
            ProfileCacheKey profileCacheKey = new ProfileCacheKey(uri, profile.getSchema(), profile.getTableName(), profile.getColumnName(), 0d);
            synchronized (ProfileCache.getInstance()) {
                dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
                if (dataCache == null) {
                    dataCache = createTimeSeriesDataCache(profileCacheKey);
                } else {
                    log.debug("Using profile {} from cache", profileCacheKey);
                }
            }
        }
        return dataCache == null ? ECollections.newBasicEList() : dataCache.get(EssimTime.dateToLocalDateTime(from), EssimTime.dateToLocalDateTime(to), dataProcessor);

    }

    private TimeSeriesDataCache createTimeSeriesDataCache(ProfileCacheKey profileCacheKey) {
        try {
            return cacheSupplier.getTimeSeriesDataCache(profileCacheKey, profile.getTimeColumnName(), profile.getStartDate(), profile.getEndDate());
        } catch (Exception e) {
            log.error("Could not create TimeSeriesDataCache for profile {}", profileCacheKey, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not implemented for DataTableProfile");
    }
}
