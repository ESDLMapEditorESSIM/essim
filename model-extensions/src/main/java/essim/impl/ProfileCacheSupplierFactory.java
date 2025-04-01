package essim.impl;

import esdl.DatabaseConfiguration;
import esdl.DatabaseTypeEnum;

import java.util.HashMap;

public class ProfileCacheSupplierFactory {
    private static HashMap<DatabaseTypeEnum, ProfileCacheSupplier> generatorMap = new HashMap<>();
    static {
        generatorMap.put(DatabaseTypeEnum.POSTGRESQL, new PostgresProfileCacheSupplier());
        generatorMap.put(DatabaseTypeEnum.INFLUXDB, new InfluxDBProfileCacheSupplier());
    }
    public static ProfileCacheSupplier getProfileDataSupplier(DatabaseConfiguration configuration) {
        return generatorMap.get(configuration.getType());
    }

}
