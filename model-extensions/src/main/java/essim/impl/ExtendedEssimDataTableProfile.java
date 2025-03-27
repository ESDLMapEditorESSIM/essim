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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;

@Slf4j
public class ExtendedEssimDataTableProfile extends ESSIMDataTableProfileImpl implements ESSIMDataTableProfile {
    private Duration aggregationPrecision;
    private DataProcessor dataProcessor;
    private Connection jdbcConnection;

    public ExtendedEssimDataTableProfile() {
        super();
    }

    @Override
    public void initProfile(Date from, Date to, Duration aggregationPrecision) {
        if (from == null && to == null && aggregationPrecision == null) {
            return;
        }
//        ProfileCacheKey profileCacheKey = new ProfileCacheKey(getHost(), getPort(), getDatabase(), getMeasurement(), getField(), getAnnualChangePercentage());

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

    private Connection getDatabaseConnection() throws SQLException {
        if (jdbcConnection == null) {
            AbstractDataConfiguration dataConfiguration = getConfiguration();
            if (!(dataConfiguration instanceof DatabaseConfiguration)) {
                throw new UnsupportedOperationException(dataConfiguration.getClass().getSimpleName() + " is not supported yet!");
            }
            String baseUri = DatabaseUriFactory.getBaseUri((DatabaseConfiguration) dataConfiguration);
            ProfileTools.DBCredentials dbCredentials = ProfileTools.getDBCredentials(baseUri);

            Properties properties = new Properties();
            properties.setProperty("user", dbCredentials.username());
            properties.setProperty("password", dbCredentials.password());
            jdbcConnection = DriverManager.getConnection(DatabaseUriFactory.create((DatabaseConfiguration) dataConfiguration), properties);
        }
        return jdbcConnection;
    }

    private void updateCache(ZonedDateTime startDate, ZonedDateTime endDate) {
        AbstractDataConfiguration dataConfiguration = getConfiguration();
        if (!(dataConfiguration instanceof DatabaseConfiguration)) {
            throw new UnsupportedOperationException(dataConfiguration.getClass().getSimpleName() + " is not supported yet!");
        }

        String baseUri = DatabaseUriFactory.getBaseUri((DatabaseConfiguration) dataConfiguration);
        ProfileCacheKey profileCacheKey = new ProfileCacheKey(baseUri, getSchema(), getTableName(), getColumnName(), null);

        synchronized (ProfileCache.getInstance()) {
            TimeSeriesDataCache dataCache = ProfileCache.getInstance().getDataCache(profileCacheKey);
            if (dataCache == null) {
                log.debug("Profile {} is not cached, proceeding to query database", profileCacheKey);
                try {
                    Connection databaseConnection = getDatabaseConnection();
                    String sql = String.format("SELECT %s as time %s as value FROM %s WHERE time >= ? AND time <= ?", getTimeColumnName(), getColumnName(), getTableName());
                    try (java.sql.PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
                        statement.setObject(1, startDate.toOffsetDateTime());
                        statement.setObject(2, endDate.toOffsetDateTime());
                        ResultSet resultSet = statement.executeQuery();
                        // TODO add new constructor to TimeSeriesDataCache which takes a resultSet and fills the cache structure
                    }

                } catch (SQLException e) {
                    log.error("Error querying database @ {}. Trying again...", baseUri, e);
                }
            } else {
                log.debug("Using profile {} from cache", profileCacheKey);
            }
        }
    }

    @Override
    public EList<ProfileElement> getProfile(Date from, Date to, Duration simulationStepLength) {
        return null;
    }
}
