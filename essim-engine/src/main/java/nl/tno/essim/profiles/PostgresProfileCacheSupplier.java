package nl.tno.essim.profiles;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import esdl.AbstractDataConfiguration;
import esdl.DatabaseConfiguration;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.CredentialUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PostgresProfileCacheSupplier implements ProfileCacheSupplier {
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d%s%s";
    private final LoadingCache<String, Connection> connectionCache;

    public PostgresProfileCacheSupplier() {
        connectionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Connection load(@NotNull String jdbcUri) throws Exception {
                        int separatorIndex = jdbcUri.indexOf("?");

                        String baseUri = separatorIndex < 0 ? jdbcUri : jdbcUri.substring(0, separatorIndex);
                        String username = "essim";
                        String password = "pgpassword";

                        Properties properties = new Properties();
                        CredentialUtils.Credentials credentials = CredentialUtils.getCredentialsFromFile(baseUri);
                        if (credentials != null) {
                            username = credentials.username();
                            password = credentials.password();
                        }
                        properties.setProperty("user", username);
                        properties.setProperty("password", password);
                        return DriverManager.getConnection(jdbcUri, properties);
                    }
                });
    }

    @Override
    public String createUri(AbstractDataConfiguration config) {
        if (!(config instanceof DatabaseConfiguration configuration)) {
            throw new IllegalStateException("Should be a DatabaseConfiguration: " + config);
        }
        String databasePart = (configuration.getDatabase() != null && !configuration.getDatabase().isEmpty()) ? "/" + configuration.getDatabase() : "";
        String tlsPart = configuration.isTls() ? "?ssl=true" : "";
        return String.format(JDBC_URL_FORMAT, configuration.getHost(), configuration.getPort(), databasePart, tlsPart);
    }

    @Override
    public TimeSeriesDataCache getTimeSeriesDataCache(ProfileCacheKey cacheKey, String timeColumnName, Date startDate, Date endDate) throws ExecutionException, SQLException {
        Connection databaseConnection = connectionCache.get(cacheKey.uri());
        String sql = String.format("SELECT %s.%s as time, %s.%s as value FROM %s", cacheKey.databaseOrSchema(), timeColumnName, cacheKey.databaseOrSchema(), cacheKey.field(), cacheKey.measurement());
        if (startDate != null) {
            sql += " WHERE time >= ? AND time <= ?";
        }
        sql += " ORDER BY time ASC";
        log.debug("Executing postgres query: {}", sql);
        try (java.sql.PreparedStatement statement = databaseConnection.prepareStatement(sql)) {
            if (startDate != null) {
                statement.setDate(1, new java.sql.Date(startDate.getTime()));
                statement.setDate(2, new java.sql.Date(endDate.getTime()));
            }
            ResultSet resultSet = statement.executeQuery();
            return new TimeSeriesDataCache(cacheKey.field(), resultSet, 0d);
        }
    }

}
