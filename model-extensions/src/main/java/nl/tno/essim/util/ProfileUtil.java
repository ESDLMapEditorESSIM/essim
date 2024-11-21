package nl.tno.essim.util;

import esdl.*;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileUtil {
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

    public static List<Double> readProfile(GenericProfile profile, Port port, Horizon horizon) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        if (profile != null) {
            Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
            Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

            EnergyAsset energyAsset = port.getEnergyasset();
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
            if (!isPercentageProfile(profile)) {
                ratedPower = 1.0;
            }
            final double power = ratedPower;

            EList<ProfileElement> profileElements = profile.getProfile(from, to,
                    Converter.toESDLDuration(horizon.getPeriod()));
            return profileElements.stream().mapToDouble(s -> s.getValue() * power).boxed().collect(Collectors.toList());
        }
        return null;
    }

    public static List<Double> readProfile(GenericProfile profile, Horizon horizon) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        if (profile != null) {
            Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
            Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

            EList<ProfileElement> profileElements = profile.getProfile(from, to,
                    Converter.toESDLDuration(horizon.getPeriod()));
            return profileElements.stream().mapToDouble(s -> s.getValue()).boxed().collect(Collectors.toList());
        }
        return null;
    }

    public static void writeProfile(Port port, EssimTime timestamp, double value) {
        EList<GenericProfile> profiles = port.getProfile();
        GenericProfile updatedProfile;

        if (profiles.isEmpty()) {
            GenericProfile profile = EssimESDLFactory.eINSTANCE.createTimeSeriesProfile();
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

    public static Double aggregateCost(List<Double> costProfile) {
        return averageOrNothing(costProfile);
    }

    public static Double sum(List<Double> energyProfile) {
        return sumOrNothing(energyProfile);
    }

    public static Double aggregateEnergy(List<Double> energyProfile) {
        return sumOrNothing(energyProfile);
    }

    public static Double aggregateGas(List<Double> gasProfile) {
        return sumOrNothing(gasProfile);
    }

    public static Double aggregatePower(List<Double> energyProfile) {
        return averageOrNothing(energyProfile);
    }

    public static Double aggregateSoC(List<Double> socProfile) {
        return averageOrNothing(socProfile);
    }

    private static Double sumOrNothing(List<Double> profile) {
        if (profile == null || profile.isEmpty()) {
            return Double.NaN;
        } else {
            return profile.stream().mapToDouble(fn -> fn.doubleValue()).sum();
        }
    }

    private static Double averageOrNothing(List<Double> profile) {
        if (profile == null || profile.isEmpty()) {
            return Double.NaN;
        } else {
            return profile.stream().mapToDouble(fn -> fn.doubleValue()).average().orElse(0.0);
        }
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
}
