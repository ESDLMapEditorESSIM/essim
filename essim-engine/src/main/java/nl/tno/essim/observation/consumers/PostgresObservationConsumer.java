package nl.tno.essim.observation.consumers;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.observation.ObservationField;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PostgresObservationConsumer implements IObservationConsumer {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory());
    private Connection schemaConnection;
    private String schemaName;
    private HashMap<String, PostgresTableCache> tables = new HashMap<>();

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
            tables = collectTableNames();

        } catch (Exception e) {
            // Log and rethrow the exception for further handling
            throw new RuntimeException("Failed to initialize schema: " + schemaName, e);
        }
    }

    public HashMap<String, PostgresTableCache> collectTableNames() {
        HashMap<String, PostgresTableCache> tableNames = new HashMap<>();
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";

        try (PreparedStatement preparedStatement = schemaConnection.prepareStatement(query)) {

            preparedStatement.setString(1, schemaName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("table_name");
                    tableNames.put(tableName, new PostgresTableCache(tableName, executor));
                }
            }

        } catch (SQLException e) {
            log.error("Error while collecting table names from schema: {}", schemaName, e);
        }

        return tableNames;
    }

    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
        String tableName = convertToValidPostgresColumnName(source.getProviderName());
        PostgresTableCache tableCache = tables.get(tableName);
        if (tableCache == null) {
            tableCache = createTable(source, observation);
        }
        tableCache.addObservation(schemaConnection, observation);
    }

    static void setPreparedStatementValue(PreparedStatement preparedStatement, int parameterIndex, Object value) throws SQLException {
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


    private PostgresTableCache createTable(IObservationProvider source, IObservation observation) {
        String tableName = convertToValidPostgresColumnName(source.getProviderName());
        try (Statement statement = schemaConnection.createStatement()) {
            StringBuilder columns = generateColumnDefinitions();
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s.%s (id BIGSERIAL PRIMARY KEY NOT NULL,time TIMESTAMP,tags JSONB,bidcurve JSONB%s)", schemaName, tableName, columns);
            log.trace("Creating table with statement: {}", sql);
            statement.executeUpdate(sql);
            PostgresTableCache tableCache = new PostgresTableCache(tableName, executor);
            tables.put(tableName, tableCache);
            log.debug("Table created: {}", tableName);
            return tableCache;
        } catch (SQLException e) {
            log.warn("Failed to create table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    static StringBuilder generateColumnDefinitions() {
        StringBuilder columns = new StringBuilder();
        Arrays.stream(ObservationField.values()).forEach(field -> {
            if (!field.equals(ObservationField.BIDCURVE)) {
                columns.append(String.format(",%s %s", convertToValidPostgresColumnName(field.getKey()), getColumnType(field.getObservationType())));
            }
        });
        return columns;
    }

    @NotNull
    private static String getColumnType(Class<?> objClass) {
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
    public void cleanup() {
        // Close the schema connection if it exists
        try {
            if (schemaConnection != null && !schemaConnection.isClosed()) {
                for (PostgresTableCache tableCache : tables.values()) {
                    if (tableCache.isDirty()) {
                        tableCache.storeObservations(schemaConnection);
                    }
                }
                executor.shutdown();
                boolean terminationSuccessful = executor.awaitTermination(1, TimeUnit.MINUTES);
                if (!terminationSuccessful) {
                    log.warn("Could not terminate postgres connection in 10 seconds!");
                }
                schemaConnection.close();
                log.debug("Schema connection closed.");
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
