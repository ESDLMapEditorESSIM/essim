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
