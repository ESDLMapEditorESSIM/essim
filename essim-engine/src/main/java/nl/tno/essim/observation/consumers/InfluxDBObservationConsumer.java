package nl.tno.essim.observation.consumers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

@Slf4j
public class InfluxDBObservationConsumer implements IObservationConsumer {

	private InfluxDB influxDbClient;
	private String database;
	private String url;

	public InfluxDBObservationConsumer(String url) {
		this.url = url;
	}

	private ThreadFactory simpleThreadFactory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName("InfluxDB writer");
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		}

	};

	@Override
	public void init(String database) {
		this.database = database;

		if (influxDbClient == null) {
			influxDbClient = InfluxDBFactory.connect(url)
					.enableBatch(10000, 10, TimeUnit.SECONDS, simpleThreadFactory).enableGzip().setDatabase(database);
		}

		if (!influxDbClient.databaseExists(database)) {
			influxDbClient.createDatabase(database);
		}

	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		Builder pointBuilder = Point.measurement(source.getProviderName());
		pointBuilder.tag(observation.getTags());
		pointBuilder.tag("simulationRun", simulationRunName);
		pointBuilder.tag("Year",
				String.valueOf(observation.getObservedAt().atZone(EssimTime.defaultTimeZone).getYear()));
		pointBuilder.fields(observation.getValues());
		pointBuilder.time(observation.getObservedAt().atZone(EssimTime.defaultTimeZone).toEpochSecond(),
				TimeUnit.SECONDS);

		influxDbClient.write(pointBuilder.build());
	}

	@Override
	public void cleanup() {
		influxDbClient.flush();
		influxDbClient.disableBatch();
		influxDbClient.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {
		org.influxdb.dto.BatchPoints.Builder batchBuilder = BatchPoints.database(database);

		List<LocalDateTime> timestamps = (List<LocalDateTime>) observation.getValues().get("Timestamps");
		if (timestamps == null) {
			log.error("No Timestamps field present. Ignoring batch write to InfluxDB!!");
			return;
		}

		for (String fieldName : observation.getValues().keySet()) {
			if (fieldName.equals("Timestamps")) {
				continue;
			}

			List<Object> listOfPoints = (List<Object>) observation.getValues().get(fieldName);

			for (int i = 0; i < listOfPoints.size(); i++) {
				Builder pointBuilder = Point.measurement(source.getProviderName());
				pointBuilder.tag(observation.getTags());
				pointBuilder.tag("simulationRun", simulationRunName);
				pointBuilder.time(timestamps.get(i).atZone(EssimTime.defaultTimeZone).toEpochSecond(),
						TimeUnit.SECONDS);
				pointBuilder.addField(fieldName, (double) listOfPoints.get(i));
				batchBuilder.point(pointBuilder.build());
			}
		}

		influxDbClient.write(batchBuilder.build());
	}
}
