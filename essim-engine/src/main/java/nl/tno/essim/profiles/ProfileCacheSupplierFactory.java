package nl.tno.essim.profiles;

import esdl.AbstractDataConfiguration;
import esdl.DatabaseConfiguration;
import esdl.DatabaseTypeEnum;

import java.util.HashMap;

public class ProfileCacheSupplierFactory {
    private static HashMap<DatabaseTypeEnum, ProfileCacheSupplier> generatorMap = new HashMap<>();

    static {
        generatorMap.put(DatabaseTypeEnum.POSTGRESQL, new PostgresProfileCacheSupplier());
        generatorMap.put(DatabaseTypeEnum.INFLUXDB, new InfluxDBProfileCacheSupplier());
    }

    public static ProfileCacheSupplier getProfileCacheSupplier(AbstractDataConfiguration configuration) {
        ProfileCacheSupplier supplier = null;
        if (configuration instanceof DatabaseConfiguration databaseConfiguration) {
            supplier = generatorMap.get(databaseConfiguration.getType());
        }
        if (supplier == null) {
            throw new UnsupportedOperationException("Profile type not (yet) supported:" + configuration);
        }
        return supplier;
    }

}
