/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */
package essim.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import esdl.Duration;
import esdl.EsdlFactory;
import esdl.ProfileElement;
import esdl.ProfileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;

@Slf4j
public class ExtendedESSIMTimeSeriesProfile extends ESSIMTimeSeriesProfileImpl {
	
	private HashMap<LocalDateTime, Double> profile = new LinkedHashMap<LocalDateTime, Double>();
	
	
	@Override
	public void initProfile(Date from, Date to, Duration step) {
		if (from == null && to == null && step == null) {
			return;
		}
		
		EssimDuration simStep = Converter.toEssimDuration(step);
		
		HashMap<LocalDateTime, Double> vals = new HashMap<LocalDateTime, Double>();
		EList<Double> profileElementList = getValues();
		long timeStepInSeconds = (long) getTimestep();
		if (profileElementList != null) {
			LocalDateTime dateTime = EssimTime.dateFromGUI(getStartDateTime());
			for (Double profileElement : profileElementList) {
				vals.put(dateTime, profileElement);
				dateTime = dateTime.plus(timeStepInSeconds, ChronoUnit.SECONDS);
			}
		}

		LocalDateTime startDate = EssimTime.dateToLocalDateTime(from);
		LocalDateTime endDate = EssimTime.dateToLocalDateTime(to);
		double val = 0.0;

		synchronized (profile) {
			for (LocalDateTime t = startDate; t.isEqual(endDate)
					|| t.isBefore(endDate); t = t.plus(simStep.getSeconds(), ChronoUnit.SECONDS)) {
				if (vals.containsKey(t)) {
					val = vals.get(t);
				}
				if (getProfileType().equals(ProfileTypeEnum.UNDEFINED)) {
					profile.put(t, Converter.toStandardizedUnits(val, getProfileQuantityAndUnit()));
				} else {
					profile.put(t, Converter.toStandardizedUnits(val, getProfileType()));
				}
			}
		}
		log.debug("Initialised TimeSeriesProfile " + this);
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
		
		LocalDateTime startTime = EssimTime.dateToLocalDateTime(from);
		LocalDateTime endTime = EssimTime.dateToLocalDateTime(to);
		EList<ProfileElement> profilePointList = ECollections.newBasicEList();

		synchronized (profile) {
			for (LocalDateTime time : profile.keySet()) {
				if ((time.isAfter(startTime) || time.isEqual(startTime)) && (time.isBefore(endTime))) {
					ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
					profileElement.setFrom(EssimTime.localDateTimeToDate(time));
					profileElement.setTo(null);
					profileElement.setValue(profile.get(time));
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
				profile.put(EssimTime.dateToLocalDateTime(profileElement.getFrom()), profileElement.getValue());
			}
		}
		return true;
	}
}
