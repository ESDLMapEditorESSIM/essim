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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import nl.tno.essim.util.DateRangeUtils;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

@Slf4j
public class ExtendedESSIMDateTimeProfile extends ESSIMDateTimeProfileImpl {
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class Segment {
        LocalDateTime start;
        LocalDateTime end;
        double value;
    }

    private final TreeMap<LocalDateTime, Segment> profile;

    public ExtendedESSIMDateTimeProfile() {
        profile = new TreeMap<>();
    }

    public void initProfile(Date start, Date end, Duration step) {

        if (start == null && end == null && step == null) {
            return;
        }

        EssimDuration simStep = Converter.toEssimDuration(step);

        HashMap<LocalDateTime, Double> vals = new HashMap<LocalDateTime, Double>();
        EList<ProfileElement> profileElementList = getElement();
        if (profileElementList != null) {
            for (ProfileElement profileElement : profileElementList) {
                vals.put(EssimTime.dateFromGUI(profileElement.getFrom()), profileElement.getValue());
            }
        }

        LocalDateTime startDate = EssimTime.dateToLocalDateTime(start);
        LocalDateTime endDate = EssimTime.dateToLocalDateTime(end);
        double val = 0.0;

        synchronized (profile) {
            for (LocalDateTime t = startDate; t.isEqual(endDate)
                    || t.isBefore(endDate); t = t.plus(simStep.getSeconds(), ChronoUnit.SECONDS)) {
                if (vals.containsKey(t)) {
                    val = vals.get(t);
                }
                Object unitType = getProfileType().equals(ProfileTypeEnum.UNDEFINED) ? getProfileQuantityAndUnit() : getProfileType();
                profile.put(t, new Segment(t, t.plus(simStep.getSeconds(), ChronoUnit.SECONDS), Converter.toStandardizedUnits(val, unitType)));
            }
        }
        log.debug("Initialised profile " + this);
    }

    /*
     * (non-Javadoc)
     *
     * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date,
     * esdl.Duration)
     */
    @Override
    public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {
        LocalDateTime start = EssimTime.dateToLocalDateTime(from);
        LocalDateTime end = EssimTime.dateToLocalDateTime(to);
        EList<ProfileElement> profilePointList = ECollections.newBasicEList();

        synchronized (profile) {
            for (var entry : profile.entrySet()) {
                Segment element = entry.getValue();

                if (DateRangeUtils.hasOverlap(start, end, element.getStart(), element.getEnd())) {
                    ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
                    profileElement.setFrom(EssimTime.localDateTimeToDate(element.getStart()));
                    profileElement.setTo(EssimTime.localDateTimeToDate(element.getEnd()));
                    profileElement.setValue(element.getValue());
                    profilePointList.add(profileElement);
                }
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
            synchronized (profile) {
                profile.put(EssimTime.dateToLocalDateTime(profileElement.getFrom()), new Segment(EssimTime.dateToLocalDateTime(profileElement.getFrom()), EssimTime.dateToLocalDateTime(profileElement.getTo()), profileElement.getValue()));
            }
        }
        return true;
    }
}
