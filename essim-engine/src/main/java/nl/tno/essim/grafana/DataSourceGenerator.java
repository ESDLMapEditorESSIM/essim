package nl.tno.essim.grafana;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSourceGenerator {
    String DS_HANDLE = "DS_ESSIM";

    String getDBName(String resourceName);

    ObjectNode createDataSource(GrafanaClient grafanaClient, String databaseName);
    void addDataSourceReference(ObjectNode node);

    static DataSourceGenerator getDataSourceGenerator(String dbUrl) {
        return new InfluxDBDataSourceGenerator(dbUrl);
    }

}
