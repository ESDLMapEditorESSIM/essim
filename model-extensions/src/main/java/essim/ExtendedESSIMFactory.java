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
