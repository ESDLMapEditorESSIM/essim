package essim;

import essim.impl.ExtendedESSIMPackageImpl;

public interface ExtendedESSIMPackage extends EssimPackage {

	ExtendedESSIMPackage eINSTANCE = ExtendedESSIMPackageImpl.init();
}
