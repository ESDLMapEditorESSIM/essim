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
package essim;

import essim.impl.ExtendedESSIMDateTimeProfile;
import essim.impl.ExtendedESSIMFactoryImpl;
import essim.impl.ExtendedESSIMInfluxDBProfile;
import essim.impl.ExtendedESSIMSingleValueProfile;

public interface ExtendedESSIMFactory extends EssimFactory {

	ExtendedESSIMFactory eINSTANCE = ExtendedESSIMFactoryImpl.init();

	@Override
	ExtendedESSIMInfluxDBProfile createESSIMInfluxDBProfile();
	
	@Override
	ExtendedESSIMSingleValueProfile createESSIMSingleValueProfile();
	
	@Override
	ExtendedESSIMDateTimeProfile createESSIMDateTimeProfile();

}
