/**
 */
package essim.impl;

import esdl.Duration;

import esdl.impl.InfluxDBProfileImpl;

import essim.ESSIMInfluxDBProfile;
import essim.ESSIMProfile;
import essim.EssimPackage;
import essim.ProfileRepetitionEnum;

import java.lang.reflect.InvocationTargetException;

import java.util.Date;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ESSIM Influx DB Profile</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link essim.impl.ESSIMInfluxDBProfileImpl#getAnnualChangePercentage <em>Annual Change Percentage</em>}</li>
 *   <li>{@link essim.impl.ESSIMInfluxDBProfileImpl#getStart <em>Start</em>}</li>
 *   <li>{@link essim.impl.ESSIMInfluxDBProfileImpl#getProfileRepetition <em>Profile Repetition</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ESSIMInfluxDBProfileImpl extends InfluxDBProfileImpl implements ESSIMInfluxDBProfile {
	/**
	 * The default value of the '{@link #getAnnualChangePercentage() <em>Annual Change Percentage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAnnualChangePercentage()
	 * @generated
	 * @ordered
	 */
	protected static final double ANNUAL_CHANGE_PERCENTAGE_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getAnnualChangePercentage() <em>Annual Change Percentage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAnnualChangePercentage()
	 * @generated
	 * @ordered
	 */
	protected double annualChangePercentage = ANNUAL_CHANGE_PERCENTAGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getStart() <em>Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStart()
	 * @generated
	 * @ordered
	 */
	protected static final Date START_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getStart() <em>Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStart()
	 * @generated
	 * @ordered
	 */
	protected Date start = START_EDEFAULT;

	/**
	 * The default value of the '{@link #getProfileRepetition() <em>Profile Repetition</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProfileRepetition()
	 * @generated
	 * @ordered
	 */
	protected static final ProfileRepetitionEnum PROFILE_REPETITION_EDEFAULT = ProfileRepetitionEnum.NONE;

	/**
	 * The cached value of the '{@link #getProfileRepetition() <em>Profile Repetition</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProfileRepetition()
	 * @generated
	 * @ordered
	 */
	protected ProfileRepetitionEnum profileRepetition = PROFILE_REPETITION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ESSIMInfluxDBProfileImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return EssimPackage.Literals.ESSIM_INFLUX_DB_PROFILE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getAnnualChangePercentage() {
		return annualChangePercentage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAnnualChangePercentage(double newAnnualChangePercentage) {
		double oldAnnualChangePercentage = annualChangePercentage;
		annualChangePercentage = newAnnualChangePercentage;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					EssimPackage.ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE, oldAnnualChangePercentage,
					annualChangePercentage));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Date getStart() {
		return start;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStart(Date newStart) {
		Date oldStart = start;
		start = newStart;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, EssimPackage.ESSIM_INFLUX_DB_PROFILE__START, oldStart,
					start));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ProfileRepetitionEnum getProfileRepetition() {
		return profileRepetition;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setProfileRepetition(ProfileRepetitionEnum newProfileRepetition) {
		ProfileRepetitionEnum oldProfileRepetition = profileRepetition;
		profileRepetition = newProfileRepetition == null ? PROFILE_REPETITION_EDEFAULT : newProfileRepetition;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					EssimPackage.ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION, oldProfileRepetition, profileRepetition));
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
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE:
			return getAnnualChangePercentage();
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__START:
			return getStart();
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION:
			return getProfileRepetition();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE:
			setAnnualChangePercentage((Double) newValue);
			return;
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__START:
			setStart((Date) newValue);
			return;
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION:
			setProfileRepetition((ProfileRepetitionEnum) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE:
			setAnnualChangePercentage(ANNUAL_CHANGE_PERCENTAGE_EDEFAULT);
			return;
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__START:
			setStart(START_EDEFAULT);
			return;
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION:
			setProfileRepetition(PROFILE_REPETITION_EDEFAULT);
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE:
			return annualChangePercentage != ANNUAL_CHANGE_PERCENTAGE_EDEFAULT;
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__START:
			return START_EDEFAULT == null ? start != null : !START_EDEFAULT.equals(start);
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION:
			return profileRepetition != PROFILE_REPETITION_EDEFAULT;
		}
		return super.eIsSet(featureID);
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
				return EssimPackage.ESSIM_INFLUX_DB_PROFILE___INIT_PROFILE__DATE_DATE_DURATION;
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
		case EssimPackage.ESSIM_INFLUX_DB_PROFILE___INIT_PROFILE__DATE_DATE_DURATION:
			initProfile((Date) arguments.get(0), (Date) arguments.get(1), (Duration) arguments.get(2));
			return null;
		}
		return super.eInvoke(operationID, arguments);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy())
			return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (annualChangePercentage: ");
		result.append(annualChangePercentage);
		result.append(", start: ");
		result.append(start);
		result.append(", profileRepetition: ");
		result.append(profileRepetition);
		result.append(')');
		return result.toString();
	}

} //ESSIMInfluxDBProfileImpl
