package nl.tno.essim.profiles;

import esdl.*;

import java.util.Date;

public class ProfileUtils {
    public static GenericProfile getEnergyProfile(Port port) {
        GenericProfile energyProfile = null;
        for (GenericProfile profile : port.getProfile()) {
            if (profile instanceof ProfileReference) {
                profile = ((ProfileReference) profile).getReference();
            }
            if (isEnergyProfile(profile) || isPowerProfile(profile) || isPercentageProfile(profile)) {
                energyProfile = profile;
            }
        }
        return energyProfile;
    }


    public static boolean isPowerProfile(GenericProfile profile) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
        if (profileQandU != null) {
            QuantityAndUnitType qu = null;
            if (profileQandU instanceof QuantityAndUnitType) {
                qu = (QuantityAndUnitType) profileQandU;
            } else if (profileQandU instanceof QuantityAndUnitReference) {
                QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
                qu = reference.getReference();
            }
            return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.POWER);
        }

        return false;
    }

    public static boolean isEnergyProfile(GenericProfile profile) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
        if (profileQandU != null) {
            QuantityAndUnitType qu = null;
            if (profileQandU instanceof QuantityAndUnitType) {
                qu = (QuantityAndUnitType) profileQandU;
            } else if (profileQandU instanceof QuantityAndUnitReference) {
                QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
                qu = reference.getReference();
            }
            return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.ENERGY);
        }

        return false;
    }

    public static boolean isPercentageProfile(GenericProfile profile) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
        if (profileQandU != null) {
            QuantityAndUnitType qu = null;
            if (profileQandU instanceof QuantityAndUnitType) {
                qu = (QuantityAndUnitType) profileQandU;
            } else if (profileQandU instanceof QuantityAndUnitReference) {
                QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
                qu = reference.getReference();
            }
            return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.COEFFICIENT);
        }

        return false;
    }

    public static boolean isSoCProfile(GenericProfile profile) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
        if (profileQandU != null) {
            QuantityAndUnitType qu = null;
            if (profileQandU instanceof QuantityAndUnitType) {
                qu = (QuantityAndUnitType) profileQandU;
            } else if (profileQandU instanceof QuantityAndUnitReference) {
                QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
                qu = reference.getReference();
            }
            return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.STATE_OF_CHARGE);
        }

        return false;
    }

    /**
     * Determines if the date ranges of two ProfileElement instances overlap.
     *
     * @param element1 The first ProfileElement.
     * @param element2 The second ProfileElement.
     * @return true if there is an overlap in the date ranges, false otherwise.
     */
    public static boolean hasOverlap(ProfileElement element1, ProfileElement element2) {
        Date from1 = element1.getFrom();
        Date from2 = element2.getFrom();
        Date to1 = element1.getTo();
        Date to2 = element2.getTo();

        // If both `to` values are null, they overlap infinitely
        if (to1 == null && to2 == null) {
            return true;
        }

        // Swap null end dates with the latest possible date
        if (to1 == null) {
            to1 = new Date(Long.MAX_VALUE);
        }
        if (to2 == null) {
            to2 = new Date(Long.MAX_VALUE);
        }

        // Compare the ranges
        return (from1.before(to2) || from1.equals(to2)) && (from2.before(to1) || from2.equals(to1));
    }
}
