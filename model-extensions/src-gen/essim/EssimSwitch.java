/**
 */
package essim;

import esdl.DatabaseProfile;
import esdl.DateTimeProfile;
import esdl.ExternalProfile;
import esdl.GenericProfile;
import esdl.InfluxDBProfile;

import esdl.SingleValue;
import esdl.StaticProfile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see essim.EssimPackage
 * @generated
 */
public class EssimSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static EssimPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EssimSwitch() {
		if (modelPackage == null) {
			modelPackage = EssimPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE: {
			ESSIMInfluxDBProfile essimInfluxDBProfile = (ESSIMInfluxDBProfile) theEObject;
			T result = caseESSIMInfluxDBProfile(essimInfluxDBProfile);
			if (result == null)
				result = caseInfluxDBProfile(essimInfluxDBProfile);
			if (result == null)
				result = caseESSIMProfile(essimInfluxDBProfile);
			if (result == null)
				result = caseDatabaseProfile(essimInfluxDBProfile);
			if (result == null)
				result = caseExternalProfile(essimInfluxDBProfile);
			if (result == null)
				result = caseGenericProfile(essimInfluxDBProfile);
			if (result == null)
				result = defaultCase(theEObject);
			return result;
		}
		case EssimPackage.ESSIM_PROFILE: {
			ESSIMProfile essimProfile = (ESSIMProfile) theEObject;
			T result = caseESSIMProfile(essimProfile);
			if (result == null)
				result = defaultCase(theEObject);
			return result;
		}
		case EssimPackage.ESSIM_DATE_TIME_PROFILE: {
			ESSIMDateTimeProfile essimDateTimeProfile = (ESSIMDateTimeProfile) theEObject;
			T result = caseESSIMDateTimeProfile(essimDateTimeProfile);
			if (result == null)
				result = caseDateTimeProfile(essimDateTimeProfile);
			if (result == null)
				result = caseESSIMProfile(essimDateTimeProfile);
			if (result == null)
				result = caseStaticProfile(essimDateTimeProfile);
			if (result == null)
				result = caseGenericProfile(essimDateTimeProfile);
			if (result == null)
				result = defaultCase(theEObject);
			return result;
		}
		case EssimPackage.ESSIM_SINGLE_VALUE_PROFILE: {
			ESSIMSingleValueProfile essimSingleValueProfile = (ESSIMSingleValueProfile) theEObject;
			T result = caseESSIMSingleValueProfile(essimSingleValueProfile);
			if (result == null)
				result = caseSingleValue(essimSingleValueProfile);
			if (result == null)
				result = caseESSIMProfile(essimSingleValueProfile);
			if (result == null)
				result = caseStaticProfile(essimSingleValueProfile);
			if (result == null)
				result = caseGenericProfile(essimSingleValueProfile);
			if (result == null)
				result = defaultCase(theEObject);
			return result;
		}
		default:
			return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>ESSIM Influx DB Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>ESSIM Influx DB Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseESSIMInfluxDBProfile(ESSIMInfluxDBProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>ESSIM Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>ESSIM Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseESSIMProfile(ESSIMProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>ESSIM Date Time Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>ESSIM Date Time Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseESSIMDateTimeProfile(ESSIMDateTimeProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>ESSIM Single Value Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>ESSIM Single Value Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseESSIMSingleValueProfile(ESSIMSingleValueProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Generic Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Generic Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseGenericProfile(GenericProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>External Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>External Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseExternalProfile(ExternalProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Database Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Database Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDatabaseProfile(DatabaseProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Influx DB Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Influx DB Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseInfluxDBProfile(InfluxDBProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Static Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Static Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseStaticProfile(StaticProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Date Time Profile</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Date Time Profile</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseDateTimeProfile(DateTimeProfile object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Single Value</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Single Value</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseSingleValue(SingleValue object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //EssimSwitch
