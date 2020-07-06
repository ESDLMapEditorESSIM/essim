package essim.impl;

import java.util.Date;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import esdl.Duration;
import esdl.EsdlFactory;
import esdl.ProfileElement;
import nl.tno.essim.util.Converter;

public class ExtendedESSIMSingleValueProfile extends ESSIMSingleValueProfileImpl {

	public void initProfile(Date from, Date to, Duration aggregationPrecision) {
		if (from == null && to == null && aggregationPrecision == null) {
			return;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.GenericProfileImpl#getProfile(java.util.Date, java.util.Date, esdl.Duration)
	 */
	/**
	 * Always returns the single ProfileElement with the SingleValue value in it, no matter what the dates are. If dates
	 * are supplied, these will be used and set in the returning ProfileElement
	 */
	@Override
	public EList<ProfileElement> getProfile(Date from, Date to, Duration aggregationPrecision) {
		ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
		profileElement.setValue(Converter.toStandardizedUnits(getValue(), getProfileType()));
		profileElement.setFrom(from);
		profileElement.setTo(to);
		return ECollections.singletonEList(profileElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.GenericProfileImpl#setProfile(org.eclipse.emf.common.util.EList)
	 */
	@Override
	public boolean setProfile(EList<ProfileElement> profileElementList) {
		if (profileElementList.size() != 1) {
			throw new IllegalArgumentException("List size should be exactly 1 for a Single Value profile");
		}
		setValue(profileElementList.get(0)
				.getValue());
		return true;
	}
}
