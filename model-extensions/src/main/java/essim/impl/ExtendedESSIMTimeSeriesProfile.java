/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 *
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
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
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
public class ExtendedESSIMTimeSeriesProfile extends ESSIMTimeSeriesProfileImpl {

    private Object lock = new Object();
    private ArrayList<Double> dataPoints;
    private long profileStart = 0;
    private long stepSize = 0;


    @Override
    public void initProfile(Date from, Date to, Duration step) {
        if (from == null && to == null && step == null) {
            return;
        }

        EssimDuration simStep = Converter.toEssimDuration(step);
        synchronized (lock) {
            stepSize = simStep.getSeconds();

            long seriesStart = EssimTime.localDateTimeToDate(EssimTime.dateFromGUI(getStartDateTime())).getTime() / 1000L;
            if (stepSize == getTimestep() && (from.getTime() - profileStart) % stepSize == 0) {
                profileStart = seriesStart;
                if (getProfileType().equals(ProfileTypeEnum.UNDEFINED)) {
                    dataPoints = new ArrayList<>(getValues().stream().map(d -> Converter.toStandardizedUnits(d, getProfileQuantityAndUnit())).collect(Collectors.toList()));
                } else {
                    dataPoints = new ArrayList<>(getValues().stream().map(d -> Converter.toStandardizedUnits(d, getProfileType())).collect(Collectors.toList()));
                }
            } else {
                profileStart = from.getTime();
                int stepCount = (int) (1 + (to.getTime() / 1000l - profileStart) / stepSize);
                dataPoints = new ArrayList<>(stepCount);
                long startTime = profileStart;
                ArrayList<Double> data = new ArrayList<>(getValues());
                for (int i = 0; i < stepCount; i++) {
                    dataPoints.add(getValueAt(startTime, stepSize, seriesStart, getTimestep(), data));
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
            if (stepSize == 0) {
                fromIndex = 0;
                toIndex = dataPoints.size();
            } else {
                fromIndex = (int) ((from.getTime() / 1000L - profileStart) / stepSize);
                toIndex = (int) ((to.getTime() / 1000L - profileStart) / stepSize);
                if (toIndex > dataPoints.size()) {
                    toIndex = dataPoints.size();
                }
                if (fromIndex < 0) {
                    fromIndex = 0;
                }
            }
            for (int i = fromIndex; i < toIndex; i++) {
                ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                profileElement.setFrom(new Date((profileStart + i * stepSize) * 1000L));
//                profileElement.setTo(new Date((profileStart + (i + 1) * stepSize) * 1000L));
                profileElement.setTo(null);
                profileElement.setValue(dataPoints.get(i));
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
                long elementSize = 0;
                if (profileElement.getTo() != null) {
                    elementSize = (profileElement.getTo().getTime() - profileElement.getFrom().getTime()) / 1000L;
                    if (stepSize != 0 && elementSize != stepSize) {
                        throw new IllegalArgumentException("Cannot add an element to a TimeSeriesProfile with a different step size");
                    }
                }
                if (dataPoints == null || dataPoints.isEmpty()) {
                    dataPoints = new ArrayList<>();
                    profileStart = profileElement.getFrom().getTime() / 1000L;
                    stepSize = elementSize;
                }
                if (stepSize == 0 && dataPoints.size() == 1) {
                    // We will determine the step size based on the distance between the first two data points
                    stepSize = profileElement.getFrom().getTime() / 1000L - profileStart;
                }
                int index = stepSize == 0 ? dataPoints.size() : (int) ((profileElement.getFrom().getTime() / 1000L - profileStart) / stepSize);
                if (index < dataPoints.size()) {
                    dataPoints.set(index, profileElement.getValue());
                } else {
                    while (index > dataPoints.size()) {
                        dataPoints.add(0.0);
                    }
                    dataPoints.add(profileElement.getValue());
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
