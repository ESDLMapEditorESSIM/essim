package nl.tno.essim.influxdb;

import okhttp3.OkHttpClient;
import org.influxdb.impl.InfluxDBImpl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InfluxConnectionProvider {
    // Cache to store InfluxDB connections and their last used time
    private static final Map<String, ConnectionEntry> connectionCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    static {
        cleaner.scheduleAtFixedRate(InfluxConnectionProvider::cleanupConnections, 15, 15, TimeUnit.MINUTES);
    }

    private static class ConnectionEntry {
        private final InfluxDBService connection;
        private Instant lastUsed;

        ConnectionEntry(InfluxDBService connection) {
            this.connection = connection;
            this.lastUsed = Instant.now();
        }

        InfluxDBService getConnection() {
            lastUsed = Instant.now();
            return connection;
        }

        Instant getLastUsed() {
            return lastUsed;
        }

        void close() {
            connection.close();
        }
    }

    // Method to get or create a cached InfluxDB connection
    public static InfluxDBService getInfluxDBService(String url) {
        return connectionCache.computeIfAbsent(url, InfluxConnectionProvider::createConnectionEntry)
                .getConnection();
    }

    private static ConnectionEntry createConnectionEntry(String url) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true);
        InfluxDBService connection = new InfluxDBService(new InfluxDBImpl(url, null, null, clientBuilder));
        return new ConnectionEntry(connection);
    }

    private static void cleanupConnections() {
        Instant threshold = Instant.now().minusSeconds(900); // 15 minutes
        connectionCache.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastUsed().isBefore(threshold)) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }
}

