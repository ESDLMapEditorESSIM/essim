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
package esdl;

import esdl.impl.EssimESDLFactoryImpl;
import essim.impl.ExtendedESSIMDateTimeProfile;
import essim.impl.ExtendedESSIMInfluxDBProfile;
import essim.impl.ExtendedESSIMSingleValueProfile;

/** This factory  overrides the generated factory and returns the new generated interfaces */
public interface EssimESDLFactory extends EsdlFactory 
{

	/** Specialize the eINSTANCE initialization with the new interface type 
	 * (overridden in the override_factory extension)
	 */
	EssimESDLFactory eINSTANCE = EssimESDLFactoryImpl.init();


	/* (non-Javadoc)
	 * @see esdl.EsdlFactory#createDateTimeProfile()
	 */
	@Override
	public ExtendedESSIMDateTimeProfile createDateTimeProfile();

	/* (non-Javadoc)
	 * @see esdl.EsdlFactory#createSingleValue()
	 */
	@Override
	public ExtendedESSIMSingleValueProfile createSingleValue();

	/* (non-Javadoc)
	 * @see esdl.EsdlFactory#createInfluxDBProfile()
	 */
	@Override
	public ExtendedESSIMInfluxDBProfile createInfluxDBProfile();

}
