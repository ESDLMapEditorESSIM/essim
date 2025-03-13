package nl.tno.essim.observation.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class PostgresTableCache {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    public final static int BULK_INSERT_COUNT = 1000;
    public final static int MAX_INSERT_DELAY = 200;

    private final String tableName;
    private final ExecutorService executor;
    private final ArrayList<IObservation> observations = new ArrayList<>(BULK_INSERT_COUNT);
    private long lastTimestamp = System.currentTimeMillis();

    public synchronized void addObservation(Connection schemaConnection, IObservation observation) {
        observations.add(observation);
        if (observations.size() >= BULK_INSERT_COUNT || System.currentTimeMillis() - lastTimestamp >= MAX_INSERT_DELAY) {
            storeObservations(schemaConnection);
        }
        lastTimestamp = System.currentTimeMillis();
    }

    public synchronized void storeObservations(Connection connection) {
        executor.submit(new BulkInsertRunnable(tableName, new ArrayList<>(observations), connection));
        observations.clear();
    }

    public synchronized boolean isDirtyAndExpired() {
        return !observations.isEmpty() && System.currentTimeMillis() - lastTimestamp >= MAX_INSERT_DELAY;
    }

    public synchronized boolean isDirty() {
        return !observations.isEmpty();
    }

    @RequiredArgsConstructor
    public static class BulkInsertRunnable implements Callable<Object> {
        private final String tableName;
        private final ArrayList<IObservation> observations;
        private final Connection connection;

        @Override
        public Object call() throws Exception {
            long start = System.currentTimeMillis();

            try {
                connection.setAutoCommit(false);
                for (IObservation observation : observations) {
                    List<Map.Entry<String, Object>> list = observation.getValues().entrySet().stream().filter(entry -> !entry.getKey().equals("bidcurve")).sorted(Map.Entry.comparingByKey()).toList();
                    String sql = String.format("INSERT INTO %s (time,tags,bidcurve,%s) VALUES (?,?::json,?::json%s)", tableName, String.join(",", list.stream().map(Map.Entry::getKey).toList()), ",?".repeat(list.size()));
//                    log.trace("Executing SQL: {}", sql);
                    PreparedStatement statement = connection.prepareStatement(sql);
                    AtomicInteger parameterIndex = new AtomicInteger(1);
                    statement.setTimestamp(parameterIndex.getAndIncrement(), Timestamp.from(observation.getObservedAt().atZone(nl.tno.essim.time.EssimTime.defaultTimeZone).toInstant()));
                    statement.setString(parameterIndex.getAndIncrement(), objectMapper.writeValueAsString(observation.getTags()));
                    statement.setString(parameterIndex.getAndIncrement(), observation.getValues().get("bidcurve") == null ? null : objectMapper.writeValueAsString(observation.getValues().get("bidcurve")));
                    for (Map.Entry<String, Object> entry : list) {
                        PostgresObservationConsumer.setPreparedStatementValue(statement, parameterIndex.getAndIncrement(), entry.getValue());
                    }
                    statement.executeUpdate();
                }
                connection.commit();
                log.trace("Inserted {} rows into table: {} in {} ms", observations.size(), tableName, System.currentTimeMillis() - start);
            } catch (SQLException | JsonProcessingException e) {
                connection.rollback();
                log.warn("Could not insert data", e);
                throw new RuntimeException(e);
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        }
    }
}
