/**
 */
package essim.impl;

import essim.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ESSIMFactoryImpl extends EFactoryImpl implements EssimFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static EssimFactory init() {
		try {
			EssimFactory theEssimFactory = (EssimFactory) EPackage.Registry.INSTANCE.getEFactory(EssimPackage.eNS_URI);
			if (theEssimFactory != null) {
				return theEssimFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new ESSIMFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ESSIMFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE:
			return createESSIMInfluxDBProfile();
		case EssimPackage.ESSIM_DATE_TIME_PROFILE:
			return createESSIMDateTimeProfile();
		case EssimPackage.ESSIM_SINGLE_VALUE_PROFILE:
			return createESSIMSingleValueProfile();
		default:
			throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
		case EssimPackage.PROFILE_REPETITION_ENUM:
			return createProfileRepetitionEnumFromString(eDataType, initialValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
		case EssimPackage.PROFILE_REPETITION_ENUM:
			return convertProfileRepetitionEnumToString(eDataType, instanceValue);
		default:
			throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ESSIMInfluxDBProfile createESSIMInfluxDBProfile() {
		ESSIMInfluxDBProfileImpl essimInfluxDBProfile = new ESSIMInfluxDBProfileImpl();
		return essimInfluxDBProfile;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ESSIMDateTimeProfile createESSIMDateTimeProfile() {
		ESSIMDateTimeProfileImpl essimDateTimeProfile = new ESSIMDateTimeProfileImpl();
		return essimDateTimeProfile;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ESSIMSingleValueProfile createESSIMSingleValueProfile() {
		ESSIMSingleValueProfileImpl essimSingleValueProfile = new ESSIMSingleValueProfileImpl();
		return essimSingleValueProfile;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ProfileRepetitionEnum createProfileRepetitionEnumFromString(EDataType eDataType, String initialValue) {
		ProfileRepetitionEnum result = ProfileRepetitionEnum.get(initialValue);
		if (result == null)
			throw new IllegalArgumentException(
					"The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertProfileRepetitionEnumToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EssimPackage getEssimPackage() {
		return (EssimPackage) getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static EssimPackage getPackage() {
		return EssimPackage.eINSTANCE;
	}

} //EssimFactoryImpl
