package nl.tno.essim.grafana;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSourceGenerator {
    String DATASOURCES_URL = "/api/datasources";

    String getDBName(String resourceName);

    ObjectNode createDataSource(GrafanaClient grafanaClient, String databaseName);

    void addDataSourceReference(ObjectNode node);

    String getJsonPath();

    static DataSourceGenerator getDataSourceGenerator(String dbUrl) {
        if (dbUrl.startsWith("postgresql://")) {
            return new PostgresDataSourceGenerator(dbUrl);
        }
        return new InfluxDBDataSourceGenerator(dbUrl);
    }

}
