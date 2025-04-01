package essim.impl;

import esdl.GenericProfile;
import esdl.InfluxDBProfile;

public class ProfileDataSupplierFactory {
    public static ProfileDataSupplier getProfileDataSupplier(GenericProfile profile) {
        if (profile instanceof InfluxDBProfile) {
            return new InfluxdbProfileDataSupplier((InfluxDBProfile) profile);
        }
        throw new UnsupportedOperationException("Profile type " + profile.getClass().getSimpleName() + " not yet implemented.");
    }
}
