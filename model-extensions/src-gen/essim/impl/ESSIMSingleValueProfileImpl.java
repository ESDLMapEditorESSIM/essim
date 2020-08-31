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
package essim.impl;

import esdl.Duration;

import esdl.impl.SingleValueImpl;

import essim.ESSIMProfile;
import essim.ESSIMSingleValueProfile;
import essim.EssimPackage;

import java.lang.reflect.InvocationTargetException;

import java.util.Date;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ESSIM Single Value Profile</b></em>'.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class ESSIMSingleValueProfileImpl extends SingleValueImpl implements ESSIMSingleValueProfile {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ESSIMSingleValueProfileImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return EssimPackage.Literals.ESSIM_SINGLE_VALUE_PROFILE;
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
				return EssimPackage.ESSIM_SINGLE_VALUE_PROFILE___INIT_PROFILE__DATE_DATE_DURATION;
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
		case EssimPackage.ESSIM_SINGLE_VALUE_PROFILE___INIT_PROFILE__DATE_DATE_DURATION:
			initProfile((Date) arguments.get(0), (Date) arguments.get(1), (Duration) arguments.get(2));
			return null;
		}
		return super.eInvoke(operationID, arguments);
	}

} //ESSIMSingleValueProfileImpl
