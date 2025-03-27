package essim.impl;

import esdl.DatabaseConfiguration;
import esdl.DatabaseTypeEnum;

import java.util.HashMap;

public class DatabaseUriFactory {
    private static HashMap<DatabaseTypeEnum, UriGenerator> generatorMap = new HashMap<>();
    static {
        generatorMap.put(DatabaseTypeEnum.POSTGRESQL, new PostgresUriGenerator());
    }

    public static String create(DatabaseConfiguration configuration) {
        return generatorMap.get(configuration.getType()).createUri(configuration);
    }

    public static String getBaseUri(DatabaseConfiguration configuration) {
        String uri = generatorMap.get(configuration.getType()).createUri(configuration);
        return uri.substring(0, uri.indexOf("?"));
    }
}
