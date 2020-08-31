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
package esdl.impl;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import esdl.EssimESDLFactory;
import esdl.EssimESDLPackage;
import essim.impl.ExtendedESSIMDateTimeProfile;
import essim.impl.ExtendedESSIMInfluxDBProfile;
import essim.impl.ExtendedESSIMSingleValueProfile;

// This factory  overrides the generated factory and returns the new generated interfaces
public class EssimESDLFactoryImpl extends EsdlFactoryImpl implements EssimESDLFactory {

	public EssimESDLFactoryImpl() {
		super();
	}

	public static EssimESDLFactory init() {
		System.err.println("Using extended ESSIM ESDL factory");
		// try {
		// Object fact = DidoFactoryImpl.init();
		// if ((fact != null) && (fact instanceof DidoFactory))
		// return (DidoFactory) fact;
		// }
		// catch (Exception exception) {
		// EcorePlugin.INSTANCE.log(exception);
		// }
		// return new DidoFactoryImplExt();
		try {
			EssimESDLFactory theDidoFactory = (EssimESDLFactory) EPackage.Registry.INSTANCE
					.getEFactory(EssimESDLPackage.eNS_URI);
			if (theDidoFactory != null) {
				return theDidoFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new EssimESDLFactoryImpl();
	}

	// @Override
	// public ExtendedDidoInfluxDBProfileImpl createDidoInfluxDBProfile() {
	// return new ExtendedDidoInfluxDBProfileImpl();
	// }
	//
	// @Override
	// public ExtendedInfluxDBObservationConsumer createInfluxDBObservationConsumer() {
	// return new ExtendedInfluxDBObservationConsumer();
	// }
	//
	// @Override
	// public ExtendedDidoDateTimeProfileImpl createDidoDateTimeProfile() {
	// return new ExtendedDidoDateTimeProfileImpl();
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.EsdlFactoryImpl#createInfluxDBProfile()
	 */
	@Override
	public ExtendedESSIMInfluxDBProfile createInfluxDBProfile() {
		return new ExtendedESSIMInfluxDBProfile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.EsdlFactoryImpl#createDateTimeProfile()
	 */
	@Override
	public ExtendedESSIMDateTimeProfile createDateTimeProfile() {
		return new ExtendedESSIMDateTimeProfile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see esdl.impl.EsdlFactoryImpl#createSingleValue()
	 */
	@Override
	public ExtendedESSIMSingleValueProfile createSingleValue() {
		return new ExtendedESSIMSingleValueProfile();
	}

}
