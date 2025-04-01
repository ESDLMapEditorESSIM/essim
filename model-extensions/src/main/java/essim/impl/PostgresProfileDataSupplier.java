package essim.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import common.ProfileCacheKey;
import common.TimeSeriesDataCache;
import esdl.DatabaseConfiguration;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PostgresProfileDataSupplier implements ProfileDataSupplier {
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d%s%s";
    private final LoadingCache<String, Connection> connectionCache;

    public PostgresProfileDataSupplier() {
        connectionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Connection load(@NotNull String jdbcUri) throws Exception {
                        String baseUri = jdbcUri.substring(0, jdbcUri.indexOf("?"));
                        ProfileTools.DBCredentials dbCredentials = ProfileTools.getDBCredentials(baseUri);

                        Properties properties = new Properties();
                        properties.setProperty("user", dbCredentials.username());
                        properties.setProperty("password", dbCredentials.password());
                        return DriverManager.getConnection(jdbcUri, properties);
                    }
                });
    }

    @Override
    public String createUri(DatabaseConfiguration configuration) {
        String databasePart = (configuration.getDatabase() != null && !configuration.getDatabase().isEmpty()) ? "/" + configuration.getDatabase() : "";
        String tlsPart = configuration.isTls() ? "?ssl=true" : "";
        return String.format(JDBC_URL_FORMAT, configuration.getHost(), configuration.getPort(), databasePart, tlsPart);
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, ZonedDateTime startDate, ZonedDateTime endDate) throws ExecutionException, SQLException {
        Connection databaseConnection = connectionCache.get(cacheKey.uri());
        String sql = String.format("SELECT %s as time %s as value FROM %s WHERE time >= ? AND time <= ?", timeColumnName, cacheKey.field(), cacheKey.measurement());
        try (java.sql.PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
            statement.setObject(1, startDate.toOffsetDateTime());
            statement.setObject(2, endDate.toOffsetDateTime());
            ResultSet resultSet = statement.executeQuery();
            return new TimeSeriesDataCache(cacheKey.field(), resultSet, 0d);
        }
    }

}
