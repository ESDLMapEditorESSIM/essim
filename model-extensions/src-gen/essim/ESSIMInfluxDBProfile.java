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

import esdl.InfluxDBProfile;

import java.util.Date;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ESSIM Influx DB Profile</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link essim.ESSIMInfluxDBProfile#getAnnualChangePercentage <em>Annual Change Percentage</em>}</li>
 *   <li>{@link essim.ESSIMInfluxDBProfile#getStart <em>Start</em>}</li>
 *   <li>{@link essim.ESSIMInfluxDBProfile#getProfileRepetition <em>Profile Repetition</em>}</li>
 * </ul>
 *
 * @see essim.EssimPackage#getESSIMInfluxDBProfile()
 * @model
 * @generated
 */
public interface ESSIMInfluxDBProfile extends InfluxDBProfile, ESSIMProfile {
	/**
	 * Returns the value of the '<em><b>Annual Change Percentage</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Annual Change Percentage</em>' attribute.
	 * @see #setAnnualChangePercentage(double)
	 * @see essim.EssimPackage#getESSIMInfluxDBProfile_AnnualChangePercentage()
	 * @model
	 * @generated
	 */
	double getAnnualChangePercentage();

	/**
	 * Sets the value of the '{@link essim.ESSIMInfluxDBProfile#getAnnualChangePercentage <em>Annual Change Percentage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Annual Change Percentage</em>' attribute.
	 * @see #getAnnualChangePercentage()
	 * @generated
	 */
	void setAnnualChangePercentage(double value);

	/**
	 * Returns the value of the '<em><b>Start</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Start</em>' attribute.
	 * @see #setStart(Date)
	 * @see essim.EssimPackage#getESSIMInfluxDBProfile_Start()
	 * @model required="true"
	 * @generated
	 */
	Date getStart();

	/**
	 * Sets the value of the '{@link essim.ESSIMInfluxDBProfile#getStart <em>Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Start</em>' attribute.
	 * @see #getStart()
	 * @generated
	 */
	void setStart(Date value);

	/**
	 * Returns the value of the '<em><b>Profile Repetition</b></em>' attribute.
	 * The literals are from the enumeration {@link essim.ProfileRepetitionEnum}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Profile Repetition</em>' attribute.
	 * @see essim.ProfileRepetitionEnum
	 * @see #setProfileRepetition(ProfileRepetitionEnum)
	 * @see essim.EssimPackage#getESSIMInfluxDBProfile_ProfileRepetition()
	 * @model
	 * @generated
	 */
	ProfileRepetitionEnum getProfileRepetition();

	/**
	 * Sets the value of the '{@link essim.ESSIMInfluxDBProfile#getProfileRepetition <em>Profile Repetition</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Profile Repetition</em>' attribute.
	 * @see essim.ProfileRepetitionEnum
	 * @see #getProfileRepetition()
	 * @generated
	 */
	void setProfileRepetition(ProfileRepetitionEnum value);

} // ESSIMInfluxDBProfile
