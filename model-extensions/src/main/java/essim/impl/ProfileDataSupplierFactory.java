package essim.impl;

import esdl.DatabaseConfiguration;
import esdl.DatabaseTypeEnum;

import java.util.HashMap;

public class ProfileDataSupplierFactory {
    private static HashMap<DatabaseTypeEnum, ProfileDataSupplier> generatorMap = new HashMap<>();
    static {
        generatorMap.put(DatabaseTypeEnum.POSTGRESQL, new PostgresProfileDataSupplier());
        generatorMap.put(DatabaseTypeEnum.INFLUXDB, new InfluxDBProfileDataSupplier());
    }
    public static ProfileDataSupplier getProfileDataSupplier(DatabaseConfiguration configuration) {
        return generatorMap.get(configuration.getType());
    }

}
