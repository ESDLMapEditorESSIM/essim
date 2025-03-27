package essim.impl;

import esdl.DatabaseConfiguration;

public class PostgresUriGenerator implements UriGenerator {
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d%s%s";

    @Override
    public String createUri(DatabaseConfiguration configuration) {
        String databasePart = (configuration.getDatabase() != null && !configuration.getDatabase().isEmpty()) ? "/" + configuration.getDatabase() : "";
        String tlsPart = configuration.isTls() ? "?ssl=true" : "";
        return String.format(JDBC_URL_FORMAT, configuration.getHost(), configuration.getPort(), databasePart, tlsPart);
    }

}
