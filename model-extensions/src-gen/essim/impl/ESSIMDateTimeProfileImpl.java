/**
 */
package essim.impl;

import esdl.Duration;

import esdl.impl.DateTimeProfileImpl;

import essim.ESSIMDateTimeProfile;
import essim.ESSIMProfile;
import essim.EssimPackage;

import java.lang.reflect.InvocationTargetException;

import java.util.Date;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ESSIM Date Time Profile</b></em>'.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class ESSIMDateTimeProfileImpl extends DateTimeProfileImpl implements ESSIMDateTimeProfile {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ESSIMDateTimeProfileImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return EssimPackage.Literals.ESSIM_DATE_TIME_PROFILE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void initProfile(Date from, Date to, Duration aggregationPrecision) {
		// TODO: implement this method
		// Ensure that you remove @generated or mark it @generated NOT
		throw new UnsupportedOperationException();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int eDerivedOperationID(int baseOperationID, Class<?> baseClass) {
		if (baseClass == ESSIMProfile.class) {
			switch (baseOperationID) {
			case EssimPackage.ESSIM_PROFILE___INIT_PROFILE__DATE_DATE_DURATION:
				return EssimPackage.ESSIM_DATE_TIME_PROFILE___INIT_PROFILE__DATE_DATE_DURATION;
			default:
				return -1;
			}
		}
		return super.eDerivedOperationID(baseOperationID, baseClass);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eInvoke(int operationID, EList<?> arguments) throws InvocationTargetException {
		switch (operationID) {
		case EssimPackage.ESSIM_DATE_TIME_PROFILE___INIT_PROFILE__DATE_DATE_DURATION:
			initProfile((Date) arguments.get(0), (Date) arguments.get(1), (Duration) arguments.get(2));
			return null;
		}
		return super.eInvoke(operationID, arguments);
	}

} //ESSIMDateTimeProfileImpl
