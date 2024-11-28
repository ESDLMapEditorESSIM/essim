package nl.tno.essim.util;

import esdl.*;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.Date;
import java.util.List;

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

    public static double readProfile(GenericProfile profile, Port port, Horizon horizon) {
        double values = readProfile(profile, horizon);
        EnergyAsset energyAsset = port.getEnergyasset();
        if (isPercentageProfile(profile)) {
            double ratedPower = 1.0;
            if (energyAsset instanceof Producer) {
                Producer producer = (Producer) energyAsset;
                ratedPower = producer.getPower();
            } else if (energyAsset instanceof Consumer) {
                Consumer consumer = (Consumer) energyAsset;
                ratedPower = consumer.getPower();
            } else if (energyAsset instanceof AbstractBasicConversion) {
                AbstractBasicConversion conversion = (AbstractBasicConversion) energyAsset;
                ratedPower = conversion.getPower();
            }
            final double power = ratedPower;
            return values * power;

        }
        return values;
    }

    public static double readProfile(GenericProfile profile, Horizon horizon) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        if (profile != null) {
            Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
            Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

            EList<ProfileElement> profileElements = profile.getProfile(from, to, Converter.toESDLDuration(horizon.getPeriod()));
            double totalDurationInMillis = 0;
            double totalValue = 0;
            double weightedSum = 0;
            for (ProfileElement profileElement : profileElements) {
                var partStart = profileElement.getFrom().before(from) ? from : profileElement.getFrom();
                var partEnd = profileElement.getTo().after(to) ? to : profileElement.getTo();
                // Calculate fraction
                long elementDuration = DateRangeUtils.getDurationInMillis(partEnd, partStart);
                double fraction =
                        (double) elementDuration / DateRangeUtils.getDurationInMillis(profileElement.getTo(), profileElement.getFrom());
                totalValue += profileElement.getValue() * fraction;
                weightedSum += profileElement.getValue() * elementDuration;
                totalDurationInMillis += elementDuration;

            }
            // For energy profiles, we sum the parts (upsampling). For all other profiles (for example power) we calculated a weighted average
            if (isEnergyProfile(profile)) {
                return totalValue;
            }
            return weightedSum / totalDurationInMillis;
        }
        return Double.NaN;
    }

    public static void writeProfile(Port port, EssimTime timestamp, double value) {
        EList<GenericProfile> profiles = port.getProfile();
        GenericProfile updatedProfile;

        if (profiles.isEmpty()) {
            TimeSeriesProfile profile = EssimESDLFactory.eINSTANCE.createTimeSeriesProfile();
            profile.setTimestep((int) timestamp.getSimulationStepLength().getSeconds());
            updatedProfile = writeProfile(profile, timestamp, value);
        } else {
            GenericProfile profile = profiles.get(0);
            if (profile instanceof ProfileReference) {
                profile = ((ProfileReference) profile).getReference();
            }
            updatedProfile = writeProfile(profile, timestamp, value);
        }
        profiles.add(updatedProfile);
    }

    public static GenericProfile writeProfile(GenericProfile profile, EssimTime timestamp, double value) {
        EList<ProfileElement> profileElementList = ECollections.newBasicEList();
        ProfileElement element = EssimESDLFactory.eINSTANCE.createProfileElement();
        element.setValue(value);
        element.setFrom(EssimTime.localDateTimeToDate(timestamp.getTime()));
        profileElementList.add(element);
        profile.setProfile(profileElementList);
        profile.setProfileType(ProfileTypeEnum.ENERGY_IN_J);

        return profile;
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

        return (profile.getProfileType().getValue() <= ProfileTypeEnum.POWER_IN_TW_VALUE)
                && (profile.getProfileType().getValue() >= ProfileTypeEnum.POWER_IN_W_VALUE);
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

        return (profile.getProfileType().getValue() <= ProfileTypeEnum.ENERGY_IN_PJ_VALUE)
                && (profile.getProfileType().getValue() >= ProfileTypeEnum.ENERGY_IN_WH_VALUE);
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

        return (profile.getProfileType().getValue() == ProfileTypeEnum.PERCENTAGE_VALUE);
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

        return profile.getProfileType().equals(ProfileTypeEnum.STATEOFCHARGE_IN_WS);
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
