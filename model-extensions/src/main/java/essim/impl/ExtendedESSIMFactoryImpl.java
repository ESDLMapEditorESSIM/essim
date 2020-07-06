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
