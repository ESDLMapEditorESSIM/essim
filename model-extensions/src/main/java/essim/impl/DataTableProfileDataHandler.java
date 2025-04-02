package essim.impl;

import esdl.Duration;
import esdl.ProfileElement;
import org.eclipse.emf.common.util.EList;

import java.util.Date;

public class DataTableProfileDataHandler implements ProfileDataHandler {
    @Override
    public EList<ProfileElement> getProfileElements(Date from, Date to, Duration aggregationPrecision) {
        return null;
    }

    @Override
    public boolean setProfileElements(EList<ProfileElement> profileElementList) {
        throw new UnsupportedOperationException("setProfileElements not implemented for DataTableProfile");
    }
}
