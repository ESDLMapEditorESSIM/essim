package essim.impl;

import esdl.*;
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
public class DateTimeProfileDataHandler implements ProfileDataHandler {
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class Segment {
        LocalDateTime start;
        LocalDateTime end;
        double value;
    }

    private final DateTimeProfile profile;
    private final TreeMap<LocalDateTime, Segment> profileData;

    public DateTimeProfileDataHandler(DateTimeProfile profile, Date start, Date end, EssimDuration simStep) {
        this.profile = profile;
        this.profileData = new TreeMap<>();


        HashMap<LocalDateTime, Double> vals = new HashMap<LocalDateTime, Double>();
        EList<ProfileElement> profileElementList = this.profile.getElement();
        if (profileElementList != null) {
            for (ProfileElement profileElement : profileElementList) {
                vals.put(EssimTime.dateFromGUI(profileElement.getFrom()), profileElement.getValue());
            }
        }

        LocalDateTime startDate = EssimTime.dateToLocalDateTime(start);
        LocalDateTime endDate = EssimTime.dateToLocalDateTime(end);
        double val = 0.0;

        synchronized (this.profile) {
            for (LocalDateTime t = startDate; t.isEqual(endDate)
                    || t.isBefore(endDate); t = t.plus(simStep.getSeconds(), ChronoUnit.SECONDS)) {
                if (vals.containsKey(t)) {
                    val = vals.get(t);
                }
                Object unitType = this.profile.getProfileType().equals(ProfileTypeEnum.UNDEFINED) ? this.profile.getProfileQuantityAndUnit() : this.profile.getProfileType();
                profileData.put(t, new Segment(t, t.plus(simStep.getSeconds(), ChronoUnit.SECONDS), Converter.toStandardizedUnits(val, unitType)));
            }
        }
        log.debug("Initialised profile {}", this);


    }

    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        LocalDateTime start = EssimTime.dateToLocalDateTime(from);
        LocalDateTime end = EssimTime.dateToLocalDateTime(to);
        EList<ProfileElement> profilePointList = ECollections.newBasicEList();

        synchronized (profileData) {
            for (var entry : profileData.entrySet()) {
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

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        for (ProfileElement profileElement : profileElementList) {
            synchronized (profileData) {
                profileData.put(EssimTime.dateToLocalDateTime(profileElement.getFrom()), new Segment(EssimTime.dateToLocalDateTime(profileElement.getFrom()), EssimTime.dateToLocalDateTime(profileElement.getTo()), profileElement.getValue()));
            }
        }
        return true;
    }
}
