package nl.tno.essim.observation.consumers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class PostgresObservationConsumer implements IObservationConsumer {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private Connection schemaConnection;
    private HashMap<String, HashSet<String>> tables = new HashMap<>();
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private String schemaName;

    public PostgresObservationConsumer(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init(String scenarioName) {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            throw new IllegalArgumentException("Scenario name cannot be null or empty");
        }
        this.schemaName = convertToValidPostgresColumnName(scenarioName);
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, properties); Statement statement = connection.createStatement()) {

            // Create the schema using the schemaName
            String createSchemaQuery = String.format("CREATE SCHEMA IF NOT EXISTS \"%s\";", schemaName);
            statement.executeUpdate(createSchemaQuery);

            log.debug("Schema created or already exists: {}", schemaName);

            // Establish a new connection to the newly created schema
            String schemaSpecificUrl = jdbcUrl + "?currentSchema=" + schemaName;
            this.schemaConnection = DriverManager.getConnection(schemaSpecificUrl, properties);

            log.debug("Connected to schema: {}", schemaName);
        } catch (Exception e) {
            // Log and rethrow the exception for further handling
            throw new RuntimeException("Failed to initialize schema: " + schemaName, e);
        }
    }


    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
        String tableName = convertToValidPostgresColumnName(source.getProviderName());
        if (!tables.containsKey(tableName)) {
            createTable(source, observation);
        }
        if (!tables.get(tableName).containsAll(observation.getValues().keySet())) {
            alterTable(tableName, source, observation);
        }
        List<Map.Entry<String, Object>> list = observation.getValues().entrySet().stream().filter(entry -> !entry.getKey().equals("bidcurve")).sorted(Map.Entry.comparingByKey()).toList();
        String sql = String.format("INSERT INTO %s (time,tags,bidcurve,%s) VALUES (?,?::json,?::json%s)", tableName, String.join(",", list.stream().map(Map.Entry::getKey).toList()), ",?".repeat(list.size()));
        log.trace("Executing SQL: {}", sql);
        try (PreparedStatement statement = schemaConnection.prepareStatement(sql)) {
            AtomicInteger parameterIndex = new AtomicInteger(1);
            statement.setTimestamp(parameterIndex.getAndIncrement(), Timestamp.from(observation.getObservedAt().atZone(nl.tno.essim.time.EssimTime.defaultTimeZone).toInstant()));
            statement.setString(parameterIndex.getAndIncrement(), objectMapper.writeValueAsString(observation.getTags()));
            statement.setString(parameterIndex.getAndIncrement(), observation.getValues().get("bidcurve") == null ? null : objectMapper.writeValueAsString(observation.getValues().get("bidcurve")));
            list.forEach(entry -> {
                try {
                    setPreparedStatementValue(statement, parameterIndex.getAndIncrement(), entry.getValue());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            statement.executeUpdate();
            log.trace("Inserted row into table: {}", tableName);
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void setPreparedStatementValue(PreparedStatement preparedStatement, int parameterIndex, Object value) throws SQLException {
        if (value == null) {
            preparedStatement.setObject(parameterIndex, null);
            return;
        }
        Class<?> objClass = value.getClass();
        if (objClass == Integer.class) {
            preparedStatement.setInt(parameterIndex, (Integer) value);
        } else if (objClass == Long.class) {
            preparedStatement.setLong(parameterIndex, (Long) value);
        } else if (objClass == Double.class) {
            preparedStatement.setDouble(parameterIndex, (Double) value);
        } else if (objClass == BigDecimal.class) {
            preparedStatement.setDouble(parameterIndex, ((BigDecimal) value).doubleValue());
        } else if (objClass == Float.class) {
            preparedStatement.setFloat(parameterIndex, (Float) value);
        } else if (objClass == Boolean.class) {
            preparedStatement.setBoolean(parameterIndex, (Boolean) value);
        } else if (objClass == Byte.class) {
            preparedStatement.setByte(parameterIndex, (Byte) value);
        } else if (objClass == Short.class) {
            preparedStatement.setShort(parameterIndex, (Short) value);
        } else if (objClass == Character.class) {
            preparedStatement.setString(parameterIndex, value.toString()); // Convert `char` to `String`
        } else if (objClass == String.class) {
            preparedStatement.setString(parameterIndex, (String) value);
        } else {
            // For unsupported types, throw an exception
            throw new UnsupportedOperationException("Unsupported object type: " + objClass.getName());
        }
    }


    private void createTable(IObservationProvider source, IObservation observation) {
        String tableName = convertToValidPostgresColumnName(source.getProviderName());
        try (Statement statement = schemaConnection.createStatement()) {
            StringBuilder columns = new StringBuilder();
            @NotNull Set<Map.Entry<String, Object>> columnSet = new HashSet<>(observation.getValues().entrySet());
            columnSet.forEach((e) -> {
                if (!e.getKey().equals("bidcurve")) {
                    columns.append(String.format(",%s %s", convertToValidPostgresColumnName(e.getKey()), getColumnType(e.getValue())));
                }
            });
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (id BIGSERIAL PRIMARY KEY NOT NULL,time TIMESTAMP,tags JSONB,bidcurve JSONB%s)", schemaName, tableName, columns);
            log.trace("Executing SQL: {}", sql);
            statement.executeUpdate(sql);
            tables.put(tableName, new HashSet<>(columnSet.stream().map(Map.Entry::getKey).toList()));
            log.debug("Table created: {}", tableName);
        } catch (SQLException e) {
            log.warn("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void alterTable(String tableName, IObservationProvider source, IObservation observation) {
        try (Statement statement = schemaConnection.createStatement()) {
            StringBuilder columns = new StringBuilder();
            @NotNull Set<Map.Entry<String, Object>> columnSet = observation.getValues().entrySet().stream().filter(k -> !k.getKey().equals("bidcurve")).collect(Collectors.toSet());
            columnSet.forEach((e) -> {
                if (!columns.isEmpty()) {
                    columns.append(",");
                }
                columns.append(String.format("ADD COLUMN IF NOT EXISTS %s %s", convertToValidPostgresColumnName(e.getKey()), getColumnType(e.getValue())));
            });
            String sql = String.format("ALTER TABLE %s.%s %s", schemaName, tableName, columns);
            log.trace("Executing SQL: {}", sql);
            statement.executeUpdate(sql);
            tables.get(tableName).addAll(columnSet.stream().map(Map.Entry::getKey).toList());
            log.debug("Table altered: {}", tableName);
        } catch (SQLException e) {
            log.warn("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private static String getColumnType(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        Class<?> objClass = o.getClass();

        if (objClass == Integer.class) {
            return "INTEGER";
        } else if (objClass == Long.class) {
            return "BIGINT";
        } else if (objClass == Double.class || objClass == BigDecimal.class) {
            return "DOUBLE PRECISION";
        } else if (objClass == Float.class) {
            return "REAL";
        } else if (objClass == Boolean.class) {
            return "BOOLEAN";
        } else if (objClass == Byte.class) {
            return "SMALLINT";
        } else if (objClass == Short.class) {
            return "SMALLINT";
        } else if (objClass == Character.class) {
            return "CHAR(1)";
        } else if (objClass == String.class) {
            return "TEXT";
        } else {
            throw new UnsupportedOperationException("Type not supported: " + objClass.getName());
        }
    }

    @Override
    public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {
        if (!tables.containsKey(source.getProviderName())) {
            createTable(source, observation);
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void cleanup() {
        // Close the schema connection if it exists
        try {
            if (schemaConnection != null && !schemaConnection.isClosed()) {
                schemaConnection.close();
                System.out.println("Schema connection closed.");
            }
        } catch (Exception e) {
            System.err.println("Failed to close schema connection: " + e.getMessage());
        }
    }

    public static String convertToValidPostgresColumnName(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }

        // Replace invalid characters with underscores and lowercase the result
        String validColumnName = input.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();

        // Ensure the name doesn't start with a digit
        if (Character.isDigit(validColumnName.charAt(0))) {
            validColumnName = "_" + validColumnName;
        }

        // Truncate to PostgreSQL's maximum identifier length (63 characters)
        if (validColumnName.length() > 63) {
            validColumnName = validColumnName.substring(0, 30) + "__" + validColumnName.substring(validColumnName.length() - 30);
        }

        return validColumnName;
    }


}
