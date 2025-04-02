package essim.impl;

import esdl.*;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import java.util.Date;

public class SingleValueProfileDataHandler implements ProfileDataHandler {
    private final SingleValue profile;

    public SingleValueProfileDataHandler(SingleValue profile) {
        this.profile = profile;
    }


    /**
     * Always returns the single ProfileElement with the SingleValue value in it, no
     * matter what the dates are. If dates are supplied, these will be used and set
     * in the returning ProfileElement
     */
    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        ProfileElement profileElement = EsdlFactory.eINSTANCE.createProfileElement();
        if (profile.getProfileType().equals(ProfileTypeEnum.UNDEFINED)) {
            profileElement.setValue(Converter.toStandardizedUnits(profile.getValue(), profile.getProfileQuantityAndUnit()));
        } else {
            profileElement.setValue(Converter.toStandardizedUnits(profile.getValue(), profile.getProfileType()));
        }
        profileElement.setFrom(from);
        profileElement.setTo(to);
        return ECollections.singletonEList(profileElement);
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not possible for SingleValueProfile");
    }

    public boolean setProfile(EList<ProfileElement> profileElementList) {
        if (profileElementList.size() != 1) {
            throw new IllegalArgumentException("List size should be exactly 1 for a Single Value profile");
        }
        profile.setValue(profileElementList.get(0).getValue());
        return true;
    }

}
