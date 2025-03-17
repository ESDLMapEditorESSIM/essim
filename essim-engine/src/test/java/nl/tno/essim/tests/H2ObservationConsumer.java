package nl.tno.essim.tests;

import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class H2ObservationConsumer implements IObservationConsumer {
	private final static String URI = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
	private ExecutorService pool;

	public H2ObservationConsumer() {
		pool = Executors.newSingleThreadExecutor();
	}

	@Override
	public void init(String scenarioName) {

	}

	enum ObservationType {
		ASSET, NETWORK,
	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		String observationSource = observation.getTags().get("assetId");
		ObservationType type = observationSource == null ? ObservationType.NETWORK : ObservationType.ASSET;
		pool.submit(new DataProcessor(type.toString(), simulationRunName, observation.getObservedAt(),
				observation.getValues(), observation.getTags()));

		try (Connection connection = DriverManager.getConnection(URI, "", "")) {

			String sql = String.format("create table if not exists %s (id serial primary key, observed timestamp%s)",
					type.toString(), createTableColumns(observationSource, observation)).toString();
			Statement statement = connection.createStatement();
			statement.execute(sql);

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private String createTableColumns(String observationSource, IObservation observation) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> value : observation.getValues().entrySet()) {
			sb.append(",");
			sb.append(value.getKey());
			sb.append(" ");
			if (value.getValue() instanceof Double) {
				sb.append("double precision");
			} else if (value.getValue() instanceof Float) {
				sb.append("real");
			} else if (value.getValue() instanceof Integer) {
				sb.append("integer");
			} else if (value.getValue() instanceof Long) {
				sb.append("bigint");
			} else {
				sb.append("text");
			}
		}
		for (Entry<String, String> value : observation.getTags().entrySet()) {
			sb.append(",");
			sb.append(value.getKey());
			sb.append(" ");
			sb.append("text");
		}
		return sb.toString();
	}

	@Override
	public void cleanup() {

	}

	class DataProcessor implements Runnable {

		private String tableName;

		public DataProcessor(String string, String simulationRunName, LocalDateTime localDateTime,
				Map<String, Object> values, Map<String, String> tags) {
			this.tableName = string;
			String sql = "insert into " + tableName + " (observed";
			String vals = " values (?";
			for (Map.Entry<String, Object> val : values.entrySet()) {
				sql += "," + val.getKey();
				vals += ",?";
			}
			for (Map.Entry<String, String> val : tags.entrySet()) {
				sql += "," + val.getKey();
				vals += ",?";
			}
			sql += ")" + vals + ")";
			int index = 1;
			try (Connection connection = DriverManager.getConnection(URI, "", "")) {
				PreparedStatement statement = connection.prepareStatement(sql);
				statement.setTimestamp(index++, Timestamp.valueOf(localDateTime));

				for (Map.Entry<String, Object> value : values.entrySet()) {
					if (value.getValue() instanceof Double) {
						statement.setDouble(index, (Double) value.getValue());
					} else if (value.getValue() instanceof Float) {
						statement.setDouble(index, (Double) value.getValue());
					} else if (value.getValue() instanceof Integer) {
						statement.setInt(index, (Integer) value.getValue());
					} else if (value.getValue() instanceof Long) {
						statement.setLong(index, (Long) value.getValue());
					} else {
						statement.setString(index, value.getValue().toString());
					}

				}
				for (Map.Entry<String, String> value : tags.entrySet()) {
					statement.setString(index, value.getValue());
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}

	}

}
