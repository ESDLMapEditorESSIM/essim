package essim.impl;

import esdl.*;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.util.Converter;
import nl.tno.essim.util.DateRangeUtils;
import nl.tno.essim.util.ProfileUtils;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.util.Date;
import java.util.HashMap;

public class ProfileDataManager {

    private final XMIResourceImpl esdlResource;
    private final EssimTime essimTime;
    private final HashMap<GenericProfile, ProfileDataHandler> profileDataSuppliers = new HashMap<>();

    public ProfileDataManager(XMIResourceImpl esdlResource, EssimTime essimTime) {
        this.esdlResource = esdlResource;
        this.essimTime = essimTime;
    }

    public void initializeProfiles() {
        for (var iterator = esdlResource.getAllContents(); iterator.hasNext(); ) {
            EObject object = iterator.next();
            if (object instanceof GenericProfile profile) {
                ProfileDataHandler profileDataSupplier = createProfileDataSupplier(profile);
                profileDataSuppliers.put(profile, profileDataSupplier);
            }
        }
    }

    public ProfileDataHandler getProfileDataSupplier(GenericProfile profile) {
        ProfileDataHandler profileDataHandler = profileDataSuppliers.get(profile);
        if (profileDataHandler == null) {
            profileDataHandler = createProfileDataSupplier(profile);
            profileDataSuppliers.put(profile, profileDataHandler);
        }
        return profileDataHandler;
    }

    private ProfileDataHandler createProfileDataSupplier(GenericProfile profile) {
        if (profile instanceof InfluxDBProfile) {
            return new InfluxdbProfileDataHandler((InfluxDBProfile) profile);
        }
        if (profile instanceof SingleValue) {
            return new SingleValueProfileDataHandler((SingleValue) profile);
        }
        if (profile instanceof TimeSeriesProfile) {
            return new TimeSeriesProfileDataHandler((TimeSeriesProfile) profile, EssimTime.localDateTimeToDate(essimTime.getSimulationStartTime()), EssimTime.localDateTimeToDate(essimTime.getSimulationEndTime()));
        }
        if (profile instanceof DateTimeProfile) {
            return new DateTimeProfileDataHandler((DateTimeProfile) profile, EssimTime.localDateTimeToDate(essimTime.getSimulationStartTime()), EssimTime.localDateTimeToDate(essimTime.getSimulationEndTime()), essimTime.getSimulationStepLength());
        }
        if (profile instanceof DataTableProfile) {
//            return new Data
        }
        throw new UnsupportedOperationException("Profile type " + profile.getClass().getSimpleName() + " not yet implemented.");
    }

    public double readProfile(GenericProfile profile, Port port, Horizon horizon) {
        double values = readProfile(profile, horizon);
        EnergyAsset energyAsset = port.getEnergyasset();
        if (ProfileUtils.isPercentageProfile(profile)) {
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

    public double readProfile(GenericProfile profile, Horizon horizon) {
        if (profile instanceof ProfileReference) {
            profile = ((ProfileReference) profile).getReference();
        }
        if (profile != null) {
            Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
            Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

            EList<ProfileElement> profileElements = getProfileDataSupplier(profile).getProfileElements(from, to, Converter.toESDLDuration(horizon.getPeriod()));
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
            if (ProfileUtils.isEnergyProfile(profile)) {
                return totalValue;
            }
            return weightedSum / totalDurationInMillis;
        }
        return Double.NaN;
    }

    public void writeProfile(Port port, EssimTime timestamp, double value) {
        EList<GenericProfile> profiles = port.getProfile();
        GenericProfile updatedProfile;

        if (profiles.isEmpty()) {
            TimeSeriesProfile profile = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
            profile.setTimestep((int) timestamp.getSimulationStepLength().getSeconds());
            profile.setStartDateTime(EssimTime.localDateTimeToDate(timestamp.getSimulationStartTime()));
            updatedProfile = writeProfile(profile, timestamp, value);
            profile.setProfileType(ProfileTypeEnum.ENERGY_IN_J);
        } else {
            GenericProfile profile = profiles.get(0);
            if (profile instanceof ProfileReference) {
                profile = ((ProfileReference) profile).getReference();
            }
            updatedProfile = writeProfile(profile, timestamp, value);
        }
        profiles.add(updatedProfile);
    }

    public GenericProfile writeProfile(GenericProfile profile, EssimTime timestamp, double value) {
        EList<ProfileElement> profileElementList = ECollections.newBasicEList();
        ProfileElement element = EsdlFactory.eINSTANCE.createProfileElement();
        element.setValue(value);
        element.setFrom(EssimTime.localDateTimeToDate(timestamp.getTime()));
        profileElementList.add(element);
        ProfileDataHandler profileDataHandler = getProfileDataSupplier(profile);
        profileDataHandler.setProfileElements(profileElementList);
        return profile;
    }

}
