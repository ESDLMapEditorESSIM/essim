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
