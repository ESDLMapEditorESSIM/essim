/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */

package nl.tno.essim.observation.consumers;

import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.BidFunction;
import nl.tno.essim.influxdb.InfluxConnectionProvider;
import nl.tno.essim.influxdb.InfluxDBService;
import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfluxDBObservationConsumer implements IObservationConsumer {

    //    private InfluxDB influxDbClient;
    private String database;
    private String influxURL;

    public InfluxDBObservationConsumer(String url) {
        this.influxURL = url;
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
        InfluxDBService influx = InfluxConnectionProvider.getInfluxDBService(influxURL);
        influx.getInfluxDB().enableBatch(10000, 10, TimeUnit.SECONDS, simpleThreadFactory).enableGzip();
        influx.createDatabase(database);

    }

    @Override
    public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
        Builder pointBuilder = Point.measurement(source.getProviderName());
        pointBuilder.tag(observation.getTags());
        pointBuilder.tag("simulationRun", simulationRunName);
        pointBuilder.tag("Year",
                String.valueOf(observation.getObservedAt().atZone(EssimTime.defaultTimeZone).getYear()));
        // We want to transform the bidcurve before putting it in the database
        Map<String, Object> valuesMap = observation.getValues();
        BidFunction bidFunction = (BidFunction) valuesMap.get("bidcurve");
        if (bidFunction != null) {
            valuesMap = new HashMap<>(observation.getValues());
            valuesMap.put("bidcurve", bidFunction.toJsonArray());
        }
        pointBuilder.fields(valuesMap);
        pointBuilder.time(observation.getObservedAt().atZone(EssimTime.defaultTimeZone).toEpochSecond(),
                TimeUnit.SECONDS);

        InfluxConnectionProvider.getInfluxDBService(influxURL).getInfluxDB().write(pointBuilder.build());
    }

    @Override
    public void cleanup() {
        // flush the content to the database. Don't close the connection, it can be reused or will be closed automatically
        InfluxDB influxDB = InfluxConnectionProvider.getInfluxDBService(influxURL).getInfluxDB();
        influxDB.flush();
        influxDB.disableBatch();
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

        InfluxConnectionProvider.getInfluxDBService(influxURL).getInfluxDB().write(batchBuilder.build());
    }
}
