package essim.impl;

import org.eclipse.emf.ecore.EPackage;

import essim.EssimPackage;
import essim.ExtendedESSIMFactory;
import essim.ExtendedESSIMPackage;

public class ExtendedESSIMPackageImpl extends EssimPackageImpl implements ExtendedESSIMPackage {

	private ExtendedESSIMPackageImpl() {
		super(eNS_URI, ExtendedESSIMFactory.eINSTANCE);
	}

	private static boolean isInited = false;

	public static ExtendedESSIMPackage init() {
		if (isInited)
			return (ExtendedESSIMPackage) EPackage.Registry.INSTANCE.getEPackage(EssimPackage.eNS_URI);

		// Obtain or create and register package
		ExtendedESSIMPackageImpl theEssimPackage = (ExtendedESSIMPackageImpl) (EPackage.Registry.INSTANCE
				.get(eNS_URI) instanceof EssimPackageImpl
						? EPackage.Registry.INSTANCE.get(eNS_URI)
						: new ExtendedESSIMPackageImpl());

		isInited = true;

		// Initialize simple dependencies
		ExtendedESSIMPackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theEssimPackage.createPackageContents();

		// Initialize created meta-data
		theEssimPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theEssimPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(EssimPackage.eNS_URI, theEssimPackage);
		return theEssimPackage;
	}
}
