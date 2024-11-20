/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */
package essim.impl;

import esdl.Duration;
import esdl.EsdlFactory;
import esdl.ProfileElement;
import esdl.ProfileTypeEnum;
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
public class ExtendedESSIMTimeSeriesProfile extends ESSIMTimeSeriesProfileImpl {

    private final Object lock = new Object();
    private long profileStart = 0;

    @Override
    public void initProfile(Date from, Date to, Duration step) {
        if (from == null && to == null && step == null) {
            return;
        }

        synchronized (lock) {
            int timeStep = (int) Converter.toEssimDuration(step).getSeconds();
            if (timeStep != 0) {
                setTimestep(timeStep);
            }


            long seriesStart = EssimTime.localDateTimeToDate(EssimTime.dateFromGUI(getStartDateTime())).getTime() / 1000L;
            if ((from.getTime() - profileStart) % getTimestep() == 0) {
                profileStart = seriesStart;
                if (getProfileType().equals(ProfileTypeEnum.UNDEFINED)) {
                    values = getValues().stream().map(d -> Converter.toStandardizedUnits(d, getProfileQuantityAndUnit())).collect(Collectors.toCollection(BasicEList::new));
                } else {
                    values = getValues().stream().map(d -> Converter.toStandardizedUnits(d, getProfileType())).collect(Collectors.toCollection(BasicEList::new));
                }
            } else {
                profileStart = from.getTime();
                int stepCount = (int) (1 + (to.getTime() / 1000l - profileStart) / getTimestep());
                values = new BasicEList<>(stepCount);
                long startTime = profileStart;
                ArrayList<Double> data = new ArrayList<>(getValues());
                for (int i = 0; i < stepCount; i++) {
                    values.add(getValueAt(startTime, getTimestep(), seriesStart, getTimestep(), data));
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

    /*
     * (non-Javadoc)
     *
     * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date,
     * esdl.Duration)
     */

    /**
     * Returns the data in this TimeSeriesProfile
     */
    @Override
    public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {
        EList<ProfileElement> profilePointList = ECollections.newBasicEList();
        synchronized (lock) {
            int fromIndex;
            int toIndex;
            if (getTimestep() == 0) {
                fromIndex = 0;
                toIndex = values.size();
            } else {
                fromIndex = (int) ((from.getTime() / 1000L - profileStart) / getTimestep());
                toIndex = (int) ((to.getTime() / 1000L - profileStart) / getTimestep());
                if (toIndex > values.size()) {
                    toIndex = values.size();
                }
                if (fromIndex < 0) {
                    fromIndex = 0;
                }
            }
            for (int i = fromIndex; i < toIndex; i++) {
                ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                profileElement.setFrom(new Date((profileStart + i * getTimestep()) * 1000L));
//                profileElement.setTo(new Date((profileStart + (i + 1) * stepSize) * 1000L));
                profileElement.setTo(null);
                profileElement.setValue(values.get(i));
                profilePointList.add(profileElement);
            }
        }
        return profilePointList;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * esdl.impl.GenericProfileImpl#setProfile(org.eclipse.emf.common.util.EList)
     */
    @Override
    public boolean setProfile(EList<ProfileElement> profileElementList) {
        for (ProfileElement profileElement : profileElementList) {
            synchronized (lock) {
                long elementStartSeconds = profileElement.getFrom().getTime() / 1000L;
                if (values == null || values.isEmpty()) {
                    values = new BasicEList<>();
                    profileStart = elementStartSeconds;
                }
                if (profileElement.getTo() != null) {
                    long elementSize = (profileElement.getTo().getTime() - profileElement.getFrom().getTime()) / 1000L;
                    if (getTimestep() == 0) {
                        setTimestep((int) elementSize);
                    } else if (elementSize != getTimestep()) {
                        throw new IllegalArgumentException("Cannot add an element to a TimeSeriesProfile with a different step size");
                    }
                }
                if (getTimestep() == 0) {
                    setTimestep(TIMESTEP_EDEFAULT);
                }
                if (elementStartSeconds < profileStart) {
                    throw new IllegalArgumentException("Cannot add an element before the start of the profile");
                }
                // timestep != 0 -> we need to add/replace an element
                if ((elementStartSeconds - profileStart) % getTimestep() != 0) {
                    throw new IllegalArgumentException("Cannot add an element to a TimeSeriesProfile with a different step size");
                }
                int elementIndex = (int) ((elementStartSeconds - profileStart) / getTimestep());
                if (elementIndex < values.size()) {
                    values.set(elementIndex, profileElement.getValue());
                } else {
                    // Fill until the element
                    while (elementIndex > values.size()) {
                        values.add(0.0);
                    }
                    values.add(profileElement.getValue());
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" +
                "name=" + this.getName() + ", " +
                "id=" + this.getId() + ", " +
                "startDateTime=" + this.getStartDateTime() + ", " +
                "timeStep=" + this.getTimestep() +
                ")"
                ;
    }
}
