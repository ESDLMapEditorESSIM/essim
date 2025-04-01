package essim.impl;

import esdl.Duration;
import esdl.ProfileElement;
import org.eclipse.emf.common.util.EList;

import java.util.Date;

public interface ProfileDataSupplier {
    EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision);
}
