package nl.tno.essim.grafana;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSourceGenerator {
    String DS_HANDLE = "DS_ESSIM";

    String getDBName(String resourceName);

    ObjectNode getDataSource(GrafanaClient grafanaClient, String databaseName);

    static DataSourceGenerator getDataSourceGenerator(String dbUrl) {
        return new InfluxDBDataSourceGenerator(dbUrl);
    }

}
