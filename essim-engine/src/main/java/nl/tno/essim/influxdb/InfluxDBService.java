package nl.tno.essim.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class InfluxDBService {

    private final InfluxDB influxDB;

    public InfluxDBService(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }



    /**
     * Execute a query against the InfluxDB database.
     *
     * @param database the database to query against
     * @param queryStatement the query string
     * @return QueryResult the result of the query
     */
    public QueryResult executeQuery(String database, String queryStatement) {
        Query query = new Query(queryStatement, database);
        return influxDB.query(query);
    }

    /**
     * Delete all entries with a certain tag for all measurements in the provided database.
     *
     * @param database the database from which to delete entries
     * @param tagKey the key of the tag to match
     * @param tagValue the value of the tag to match
     */
    public void deleteEntriesWithTag(String database, String tagKey, String tagValue) {
        String deleteQuery = String.format("DELETE WHERE %s = '%s'", tagKey, tagValue);
        Query query = new Query(deleteQuery, database);
        influxDB.query(query);
    }

    /**
     * Close the InfluxDB connection.
     */
    public void close() {
        influxDB.close();
    }
}