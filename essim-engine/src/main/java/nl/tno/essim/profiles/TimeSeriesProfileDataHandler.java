package nl.tno.essim.profiles;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
public class TimeSeriesProfileDataHandler implements ProfileDataHandler {
    // This values comes from TimeSeriesProfileImpl!
    protected static final int TIMESTEP_EDEFAULT = 3600;

    private final Object lock = new Object();
    private BasicEList<Double> values;
    private long profileStart = 0;
    private final TimeSeriesProfile profile;

    public TimeSeriesProfileDataHandler(TimeSeriesProfile profile, Date from, Date to) {
        this.profile = profile;
        if (from == null && to == null) {
            return;
        }
        synchronized (lock) {
            long seriesStart = EssimTime.localDateTimeToDate(EssimTime.dateFromGUI(profile.getStartDateTime())).getTime() / 1000L;
            if ((from.getTime() - profileStart) % profile.getTimestep() == 0) {
                profileStart = seriesStart;
                Object unitType = profile.getProfileType().equals(ProfileTypeEnum.UNDEFINED) ? profile.getProfileQuantityAndUnit() : profile.getProfileType();
                values = profile.getValues().stream().map(d -> Converter.toStandardizedUnits(d, unitType)).collect(Collectors.toCollection(BasicEList::new));
            } else {
                profileStart = from.getTime();
                int stepCount = (int) (1 + (to.getTime() / 1000L - profileStart) / profile.getTimestep());
                values = new BasicEList<>(stepCount);
                long startTime = profileStart;
                ArrayList<Double> data = new ArrayList<>(profile.getValues());
                for (int i = 0; i < stepCount; i++) {
                    values.add(getValueAt(startTime, profile.getTimestep(), seriesStart, profile.getTimestep(), data));
                }
            }
        }
        log.debug("Initialised TimeSeriesProfile " + this.toString());

    }

    private static double getValueAt(long atTime, long atStep, long seriesStart, long seriesStep, ArrayList<Double> data) {
        int index = (int) ((atTime - seriesStart) / seriesStep);
        long stepPart = (atTime - seriesStart) % seriesStep;
        double value = 0;
        do {
            if (stepPart > atStep) {
                stepPart = atStep;
            }
            value += data.get(index) * (double) stepPart / seriesStep;
            atStep -= stepPart;
            index++;
            stepPart = seriesStep;
        } while (atStep > 0);
        return value;
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        for (ProfileElement profileElement : profileElementList) {
            synchronized (lock) {
                long elementStartSeconds = profileElement.getFrom().getTime() / 1000L;
                if (values == null || values.isEmpty()) {
                    values = new BasicEList<>();
                    profileStart = elementStartSeconds;
                }
                if (profileElement.getTo() != null) {
                    long elementSize = (profileElement.getTo().getTime() - profileElement.getFrom().getTime()) / 1000L;
                    if (profile.getTimestep() == 0) {
                        profile.setTimestep((int) elementSize);
                    } else if (elementSize != profile.getTimestep()) {
                        throw new IllegalArgumentException("Cannot add an element to a TimeSeriesProfile with a different step size");
                    }
                }
                if (profile.getTimestep() == 0) {
                    profile.setTimestep(TIMESTEP_EDEFAULT);
                }
                if (elementStartSeconds < profileStart) {
                    throw new IllegalArgumentException("Cannot add an element before the start of the profile");
                }
                // timestep != 0 -> we need to add/replace an element
                if ((elementStartSeconds - profileStart) % profile.getTimestep() != 0) {
                    throw new IllegalArgumentException("Cannot add an element to a TimeSeriesProfile with a different step size");
                }
                int elementIndex = (int) ((elementStartSeconds - profileStart) / profile.getTimestep());

                double profileValue = Converter.fromDidoUnit(profileElement.getValue(), profile.getProfileType());
                if (elementIndex < values.size()) {
                    values.set(elementIndex, profileElement.getValue());
                    profile.getValues().set(elementIndex, profileValue);
                } else {
                    // Fill until the element
                    while (elementIndex > values.size()) {
                        values.add(0.0);
                    }
                    values.add(profileElement.getValue());
                    profile.getValues().add(profileValue);
                }
            }
        }
        return true;
    }

    @Override
    public ProfileCacheKey getProfileCacheKey() {
        return null;
    }


    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        EList<ProfileElement> profilePointList = ECollections.newBasicEList();
        synchronized (lock) {
            int fromIndex = (int) ((from.getTime() / 1000L - profileStart) / profile.getTimestep());
            long relTo = to.getTime() / 1000L - profileStart;
            int toIndex = (int) (relTo / profile.getTimestep());
            if (relTo % profile.getTimestep() != 0) {
                toIndex++;
            }

            if (toIndex > values.size()) {
                toIndex = values.size();
            }
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            for (int i = fromIndex; i < toIndex; i++) {
                ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                profileElement.setFrom(new Date((profileStart + (long) i * profile.getTimestep()) * 1000L));
                profileElement.setTo(new Date((profileStart + (long) (i + 1) * profile.getTimestep()) * 1000L));
                profileElement.setValue(values.get(i));
                profilePointList.add(profileElement);
            }
        }
        return profilePointList;
    }
}
