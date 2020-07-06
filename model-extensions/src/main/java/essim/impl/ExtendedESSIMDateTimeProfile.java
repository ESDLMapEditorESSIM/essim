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
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;

@Slf4j
public class ExtendedESSIMDateTimeProfile extends ESSIMDateTimeProfileImpl {

	private HashMap<LocalDateTime, Double> profile;

	public ExtendedESSIMDateTimeProfile() {
		profile = new LinkedHashMap<LocalDateTime, Double>();
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
		for (LocalDateTime t = startDate; t.isEqual(endDate)
				|| t.isBefore(endDate); t = t.plus(simStep.getSeconds(), ChronoUnit.SECONDS)) {
			if (vals.containsKey(t)) {
				val = vals.get(t);
			}
			profile.put(t, val);
		}
		log.debug("Initialised profile " + this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date, esdl.Duration)
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
	 * @see esdl.impl.GenericProfileImpl#setProfile(org.eclipse.emf.common.util.EList)
	 */
	@Override
	public boolean setProfile(EList<ProfileElement> profileElementList) {
		// LocalDateTime startTime = DidoTime.dateToLocalDateTime(start);
		// DidoDuration didoDuration = Converter.toDidoDuration(duration);

		// LocalDateTime endTime = startTime.plus(didoDuration.getAmount(), didoDuration.getUnit());
		// long seconds = ChronoUnit.SECONDS.between(startTime, endTime);
		// long period = seconds / profileItem.size();
		// for(int i=0; i<profileItem.size(); i++) {
		// LocalDateTime time = startTime.plus(i*period, ChronoUnit.SECONDS);
		// profile.put(time, profileItem.get(i));
		// }

		for (ProfileElement profileElement : profileElementList) {
			synchronized (profile) {
				profile.put(EssimTime.dateToLocalDateTime(profileElement.getFrom()), profileElement.getValue());
			}
		}
		return true;
	}
}
