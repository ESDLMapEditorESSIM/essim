package nl.tno.essim.profiles;

import esdl.Duration;
import esdl.ProfileElement;
import org.eclipse.emf.common.util.EList;

import java.util.Date;

public interface ProfileDataHandler {
    EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision);
    boolean setProfileElements(EList<ProfileElement> profileElementList);
}
