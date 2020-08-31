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
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import essim.EssimPackage;
import essim.ExtendedESSIMFactory;

public class ExtendedESSIMFactoryImpl extends ESSIMFactoryImpl implements ExtendedESSIMFactory {

	public ExtendedESSIMFactoryImpl() {
		super();
	}

	public static ExtendedESSIMFactory init() {
		System.err.println("Using extended factory");
		try {
			ExtendedESSIMFactory theESSIMFactory = (ExtendedESSIMFactory) EPackage.Registry.INSTANCE
					.getEFactory(EssimPackage.eNS_URI);
			if (theESSIMFactory != null) {
				return theESSIMFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new ExtendedESSIMFactoryImpl();
	}

	@Override
	public ExtendedESSIMInfluxDBProfile createESSIMInfluxDBProfile() {
		return new ExtendedESSIMInfluxDBProfile();
	}

	@Override
	public ExtendedESSIMSingleValueProfile createESSIMSingleValueProfile() {
		return new ExtendedESSIMSingleValueProfile();
	}

	@Override
	public ExtendedESSIMDateTimeProfile createESSIMDateTimeProfile() {
		return new ExtendedESSIMDateTimeProfile();
	}

}
