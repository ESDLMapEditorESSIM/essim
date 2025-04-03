package nl.tno.essim.profiles;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import esdl.AbstractDataConfiguration;
import nl.tno.essim.commons.CredentialUtils;
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

public class PostgresProfileCacheSupplier implements ProfileCacheSupplier {
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d%s%s";
    private final LoadingCache<String, Connection> connectionCache;

    public PostgresProfileCacheSupplier() {
        connectionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Connection load(@NotNull String jdbcUri) throws Exception {
                        String baseUri = jdbcUri.substring(0, jdbcUri.indexOf("?"));
                        CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(baseUri);

                        Properties properties = new Properties();
                        properties.setProperty("user", credentials.username());
                        properties.setProperty("password", credentials.password());
                        return DriverManager.getConnection(jdbcUri, properties);
                    }
                });
    }

    @Override
    public String createUri(AbstractDataConfiguration config) {
        if (!(config instanceof DatabaseConfiguration configuration)) {
            throw new IllegalStateException("Should be a DatabaseConfiguration: " +config);
        }
        String databasePart = (configuration.getDatabase() != null && !configuration.getDatabase().isEmpty()) ? "/" + configuration.getDatabase() : "";
        String tlsPart = configuration.isTls() ? "?ssl=true" : "";
        return String.format(JDBC_URL_FORMAT, configuration.getHost(), configuration.getPort(), databasePart, tlsPart);
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName) throws ExecutionException, SQLException {
        Connection databaseConnection = connectionCache.get(cacheKey.uri());
        String sql = String.format("SELECT %s as time %s as value FROM %s ORDER BY time ASC", timeColumnName, cacheKey.field(), cacheKey.measurement());
        try (java.sql.PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            return new TimeSeriesDataCache(cacheKey.field(), resultSet, 0d);
        }
    }

}
