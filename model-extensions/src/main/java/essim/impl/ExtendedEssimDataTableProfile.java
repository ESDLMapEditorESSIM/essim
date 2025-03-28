package essim.impl;

import common.DataProcessor;
import common.ProfileCache;
import common.ProfileCacheKey;
import common.TimeSeriesDataCache;
import esdl.AbstractDataConfiguration;
import esdl.DatabaseConfiguration;
import esdl.Duration;
import esdl.ProfileElement;
import essim.ESSIMDataTableProfile;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimTime;
import org.eclipse.emf.common.util.EList;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ExtendedEssimDataTableProfile extends ESSIMDataTableProfileImpl implements ESSIMDataTableProfile {
    private Duration aggregationPrecision;
    private DataProcessor dataProcessor;
    private Connection jdbcConnection;
    private TimeSeriesDataCache dataCache;

    public ExtendedEssimDataTableProfile() {
        super();
    }

    @Override
    public void initProfile(Date from, Date to, Duration aggregationPrecision) {
        if (from == null && to == null && aggregationPrecision == null) {
            return;
        }
        this.aggregationPrecision = aggregationPrecision;

        Date profileStartDate = getStartDate();
        Date profileEndDate = getEndDate();
        ZonedDateTime startDate;
        ZonedDateTime endDate;

        if (profileStartDate != null) {
            LocalDateTime startDateFromGUI = EssimTime.dateToLocalDateTime(profileStartDate);
            LocalDateTime endDateFromGUI;
            if (profileEndDate != null) {
                endDateFromGUI = EssimTime.dateToLocalDateTime(profileEndDate);
            } else {
                endDateFromGUI = startDateFromGUI.plusYears(1L);
            }
            startDate = startDateFromGUI.atZone(EssimTime.defaultTimeZone);
            endDate = endDateFromGUI.atZone(EssimTime.defaultTimeZone);
        } else {
            startDate = from.toInstant().atZone(ZoneId.systemDefault());
            endDate = to.toInstant().atZone(ZoneId.systemDefault());
        }
        if (getProfileQuantityAndUnit() != null) {
            dataProcessor = new DataProcessor(getProfileQuantityAndUnit(), getMultiplier());
        } else {
            dataProcessor = new DataProcessor(getProfileType(), getMultiplier());
        }
        updateCache(startDate, endDate);
    }

    private void updateCache(ZonedDateTime startDate, ZonedDateTime endDate) {
        AbstractDataConfiguration dataConfiguration = getConfiguration();
        if (!(dataConfiguration instanceof DatabaseConfiguration)) {
            throw new UnsupportedOperationException(dataConfiguration.getClass().getSimpleName() + " is not supported yet!");
        }

        ProfileDataSupplier profileDataSupplier = ProfileDataSupplierFactory.getProfileDataSupplier((DatabaseConfiguration) dataConfiguration);
        ProfileCacheKey profileCacheKey = new ProfileCacheKey(profileDataSupplier.createUri((DatabaseConfiguration) dataConfiguration), getSchema(), getTableName(), getColumnName(), 0d);

        synchronized (ProfileCache.getInstance()) {
            dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
            if (dataCache == null) {
                log.debug("Profile {} is not cached, proceeding to query database", profileCacheKey);
                try {
                    profileDataSupplier.getTimeSeriesDataCache(profileCacheKey, getTimeColumnName(), startDate, endDate);

                } catch (SQLException | ExecutionException e) {
                    log.error("Error querying database @ {}. Trying again...", profileCacheKey.uri(), e);
                }
            } else {
                log.debug("Using profile {} from cache", profileCacheKey);
            }
        }
    }

    @Override
    public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {
        if (this.aggregationPrecision != null && !this.aggregationPrecision.getDurationUnit().equals(aggregationPrecision.getDurationUnit()) && (this.aggregationPrecision.getValue() == aggregationPrecision.getValue())) {
            log.warn("Different aggregation precisions are not (yet) supported and ignored!");
        }
        return dataCache.get(EssimTime.dateToLocalDateTime(from), EssimTime.dateToLocalDateTime(to), dataProcessor);
    }

}