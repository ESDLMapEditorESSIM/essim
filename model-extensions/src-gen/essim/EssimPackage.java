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

import esdl.EsdlPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see essim.EssimFactory
 * @model kind="package"
 * @generated
 */
public interface EssimPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "essim";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://www.tno.nl/essim";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "essim";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	EssimPackage eINSTANCE = essim.impl.EssimPackageImpl.init();

	/**
	 * The meta object id for the '{@link essim.impl.ESSIMInfluxDBProfileImpl <em>ESSIM Influx DB Profile</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see essim.impl.ESSIMInfluxDBProfileImpl
	 * @see essim.impl.EssimPackageImpl#getESSIMInfluxDBProfile()
	 * @generated
	 */
	int ESSIM_INFLUX_DB_PROFILE = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__NAME = EsdlPackage.INFLUX_DB_PROFILE__NAME;

	/**
	 * The feature id for the '<em><b>Profile Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__PROFILE_TYPE = EsdlPackage.INFLUX_DB_PROFILE__PROFILE_TYPE;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__ID = EsdlPackage.INFLUX_DB_PROFILE__ID;

	/**
	 * The feature id for the '<em><b>Data Source</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__DATA_SOURCE = EsdlPackage.INFLUX_DB_PROFILE__DATA_SOURCE;

	/**
	 * The feature id for the '<em><b>Profile Quantity And Unit</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__PROFILE_QUANTITY_AND_UNIT = EsdlPackage.INFLUX_DB_PROFILE__PROFILE_QUANTITY_AND_UNIT;

	/**
	 * The feature id for the '<em><b>Interpolation Method</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__INTERPOLATION_METHOD = EsdlPackage.INFLUX_DB_PROFILE__INTERPOLATION_METHOD;

	/**
	 * The feature id for the '<em><b>Multiplier</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__MULTIPLIER = EsdlPackage.INFLUX_DB_PROFILE__MULTIPLIER;

	/**
	 * The feature id for the '<em><b>Start Date</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__START_DATE = EsdlPackage.INFLUX_DB_PROFILE__START_DATE;

	/**
	 * The feature id for the '<em><b>End Date</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__END_DATE = EsdlPackage.INFLUX_DB_PROFILE__END_DATE;

	/**
	 * The feature id for the '<em><b>Host</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__HOST = EsdlPackage.INFLUX_DB_PROFILE__HOST;

	/**
	 * The feature id for the '<em><b>Port</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__PORT = EsdlPackage.INFLUX_DB_PROFILE__PORT;

	/**
	 * The feature id for the '<em><b>Database</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__DATABASE = EsdlPackage.INFLUX_DB_PROFILE__DATABASE;

	/**
	 * The feature id for the '<em><b>Filters</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__FILTERS = EsdlPackage.INFLUX_DB_PROFILE__FILTERS;

	/**
	 * The feature id for the '<em><b>Measurement</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__MEASUREMENT = EsdlPackage.INFLUX_DB_PROFILE__MEASUREMENT;

	/**
	 * The feature id for the '<em><b>Field</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__FIELD = EsdlPackage.INFLUX_DB_PROFILE__FIELD;

	/**
	 * The feature id for the '<em><b>Annual Change Percentage</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE = EsdlPackage.INFLUX_DB_PROFILE_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Start</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__START = EsdlPackage.INFLUX_DB_PROFILE_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Profile Repetition</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION = EsdlPackage.INFLUX_DB_PROFILE_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>ESSIM Influx DB Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE_FEATURE_COUNT = EsdlPackage.INFLUX_DB_PROFILE_FEATURE_COUNT + 3;

	/**
	 * The operation id for the '<em>Get Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE___GET_PROFILE__DATE_DATE_DURATION = EsdlPackage.INFLUX_DB_PROFILE___GET_PROFILE__DATE_DATE_DURATION;

	/**
	 * The operation id for the '<em>Set Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE___SET_PROFILE__ELIST = EsdlPackage.INFLUX_DB_PROFILE___SET_PROFILE__ELIST;

	/**
	 * The operation id for the '<em>Init Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE___INIT_PROFILE__DATE_DATE_DURATION = EsdlPackage.INFLUX_DB_PROFILE_OPERATION_COUNT + 0;

	/**
	 * The number of operations of the '<em>ESSIM Influx DB Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_INFLUX_DB_PROFILE_OPERATION_COUNT = EsdlPackage.INFLUX_DB_PROFILE_OPERATION_COUNT + 1;

	/**
	 * The meta object id for the '{@link essim.ESSIMProfile <em>ESSIM Profile</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see essim.ESSIMProfile
	 * @see essim.impl.EssimPackageImpl#getESSIMProfile()
	 * @generated
	 */
	int ESSIM_PROFILE = 1;

	/**
	 * The number of structural features of the '<em>ESSIM Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_PROFILE_FEATURE_COUNT = 0;

	/**
	 * The operation id for the '<em>Init Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_PROFILE___INIT_PROFILE__DATE_DATE_DURATION = 0;

	/**
	 * The number of operations of the '<em>ESSIM Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_PROFILE_OPERATION_COUNT = 1;

	/**
	 * The meta object id for the '{@link essim.impl.ESSIMDateTimeProfileImpl <em>ESSIM Date Time Profile</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see essim.impl.ESSIMDateTimeProfileImpl
	 * @see essim.impl.EssimPackageImpl#getESSIMDateTimeProfile()
	 * @generated
	 */
	int ESSIM_DATE_TIME_PROFILE = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__NAME = EsdlPackage.DATE_TIME_PROFILE__NAME;

	/**
	 * The feature id for the '<em><b>Profile Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__PROFILE_TYPE = EsdlPackage.DATE_TIME_PROFILE__PROFILE_TYPE;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__ID = EsdlPackage.DATE_TIME_PROFILE__ID;

	/**
	 * The feature id for the '<em><b>Data Source</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__DATA_SOURCE = EsdlPackage.DATE_TIME_PROFILE__DATA_SOURCE;

	/**
	 * The feature id for the '<em><b>Profile Quantity And Unit</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__PROFILE_QUANTITY_AND_UNIT = EsdlPackage.DATE_TIME_PROFILE__PROFILE_QUANTITY_AND_UNIT;

	/**
	 * The feature id for the '<em><b>Interpolation Method</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__INTERPOLATION_METHOD = EsdlPackage.DATE_TIME_PROFILE__INTERPOLATION_METHOD;

	/**
	 * The feature id for the '<em><b>Element</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE__ELEMENT = EsdlPackage.DATE_TIME_PROFILE__ELEMENT;

	/**
	 * The number of structural features of the '<em>ESSIM Date Time Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE_FEATURE_COUNT = EsdlPackage.DATE_TIME_PROFILE_FEATURE_COUNT + 0;

	/**
	 * The operation id for the '<em>Get Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE___GET_PROFILE__DATE_DATE_DURATION = EsdlPackage.DATE_TIME_PROFILE___GET_PROFILE__DATE_DATE_DURATION;

	/**
	 * The operation id for the '<em>Set Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE___SET_PROFILE__ELIST = EsdlPackage.DATE_TIME_PROFILE___SET_PROFILE__ELIST;

	/**
	 * The operation id for the '<em>Init Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE___INIT_PROFILE__DATE_DATE_DURATION = EsdlPackage.DATE_TIME_PROFILE_OPERATION_COUNT + 0;

	/**
	 * The number of operations of the '<em>ESSIM Date Time Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_DATE_TIME_PROFILE_OPERATION_COUNT = EsdlPackage.DATE_TIME_PROFILE_OPERATION_COUNT + 1;

	/**
	 * The meta object id for the '{@link essim.impl.ESSIMSingleValueProfileImpl <em>ESSIM Single Value Profile</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see essim.impl.ESSIMSingleValueProfileImpl
	 * @see essim.impl.EssimPackageImpl#getESSIMSingleValueProfile()
	 * @generated
	 */
	int ESSIM_SINGLE_VALUE_PROFILE = 3;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__NAME = EsdlPackage.SINGLE_VALUE__NAME;

	/**
	 * The feature id for the '<em><b>Profile Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__PROFILE_TYPE = EsdlPackage.SINGLE_VALUE__PROFILE_TYPE;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__ID = EsdlPackage.SINGLE_VALUE__ID;

	/**
	 * The feature id for the '<em><b>Data Source</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__DATA_SOURCE = EsdlPackage.SINGLE_VALUE__DATA_SOURCE;

	/**
	 * The feature id for the '<em><b>Profile Quantity And Unit</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__PROFILE_QUANTITY_AND_UNIT = EsdlPackage.SINGLE_VALUE__PROFILE_QUANTITY_AND_UNIT;

	/**
	 * The feature id for the '<em><b>Interpolation Method</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__INTERPOLATION_METHOD = EsdlPackage.SINGLE_VALUE__INTERPOLATION_METHOD;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE__VALUE = EsdlPackage.SINGLE_VALUE__VALUE;

	/**
	 * The number of structural features of the '<em>ESSIM Single Value Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE_FEATURE_COUNT = EsdlPackage.SINGLE_VALUE_FEATURE_COUNT + 0;

	/**
	 * The operation id for the '<em>Get Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE___GET_PROFILE__DATE_DATE_DURATION = EsdlPackage.SINGLE_VALUE___GET_PROFILE__DATE_DATE_DURATION;

	/**
	 * The operation id for the '<em>Set Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE___SET_PROFILE__ELIST = EsdlPackage.SINGLE_VALUE___SET_PROFILE__ELIST;

	/**
	 * The operation id for the '<em>Init Profile</em>' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE___INIT_PROFILE__DATE_DATE_DURATION = EsdlPackage.SINGLE_VALUE_OPERATION_COUNT + 0;

	/**
	 * The number of operations of the '<em>ESSIM Single Value Profile</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ESSIM_SINGLE_VALUE_PROFILE_OPERATION_COUNT = EsdlPackage.SINGLE_VALUE_OPERATION_COUNT + 1;

	/**
	 * The meta object id for the '{@link essim.ProfileRepetitionEnum <em>Profile Repetition Enum</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see essim.ProfileRepetitionEnum
	 * @see essim.impl.EssimPackageImpl#getProfileRepetitionEnum()
	 * @generated
	 */
	int PROFILE_REPETITION_ENUM = 4;

	/**
	 * Returns the meta object for class '{@link essim.ESSIMInfluxDBProfile <em>ESSIM Influx DB Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>ESSIM Influx DB Profile</em>'.
	 * @see essim.ESSIMInfluxDBProfile
	 * @generated
	 */
	EClass getESSIMInfluxDBProfile();

	/**
	 * Returns the meta object for the attribute '{@link essim.ESSIMInfluxDBProfile#getAnnualChangePercentage <em>Annual Change Percentage</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Annual Change Percentage</em>'.
	 * @see essim.ESSIMInfluxDBProfile#getAnnualChangePercentage()
	 * @see #getESSIMInfluxDBProfile()
	 * @generated
	 */
	EAttribute getESSIMInfluxDBProfile_AnnualChangePercentage();

	/**
	 * Returns the meta object for the attribute '{@link essim.ESSIMInfluxDBProfile#getStart <em>Start</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Start</em>'.
	 * @see essim.ESSIMInfluxDBProfile#getStart()
	 * @see #getESSIMInfluxDBProfile()
	 * @generated
	 */
	EAttribute getESSIMInfluxDBProfile_Start();

	/**
	 * Returns the meta object for the attribute '{@link essim.ESSIMInfluxDBProfile#getProfileRepetition <em>Profile Repetition</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Profile Repetition</em>'.
	 * @see essim.ESSIMInfluxDBProfile#getProfileRepetition()
	 * @see #getESSIMInfluxDBProfile()
	 * @generated
	 */
	EAttribute getESSIMInfluxDBProfile_ProfileRepetition();

	/**
	 * Returns the meta object for class '{@link essim.ESSIMProfile <em>ESSIM Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>ESSIM Profile</em>'.
	 * @see essim.ESSIMProfile
	 * @generated
	 */
	EClass getESSIMProfile();

	/**
	 * Returns the meta object for the '{@link essim.ESSIMProfile#initProfile(java.util.Date, java.util.Date, esdl.Duration) <em>Init Profile</em>}' operation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the '<em>Init Profile</em>' operation.
	 * @see essim.ESSIMProfile#initProfile(java.util.Date, java.util.Date, esdl.Duration)
	 * @generated
	 */
	EOperation getESSIMProfile__InitProfile__Date_Date_Duration();

	/**
	 * Returns the meta object for class '{@link essim.ESSIMDateTimeProfile <em>ESSIM Date Time Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>ESSIM Date Time Profile</em>'.
	 * @see essim.ESSIMDateTimeProfile
	 * @generated
	 */
	EClass getESSIMDateTimeProfile();

	/**
	 * Returns the meta object for class '{@link essim.ESSIMSingleValueProfile <em>ESSIM Single Value Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>ESSIM Single Value Profile</em>'.
	 * @see essim.ESSIMSingleValueProfile
	 * @generated
	 */
	EClass getESSIMSingleValueProfile();

	/**
	 * Returns the meta object for enum '{@link essim.ProfileRepetitionEnum <em>Profile Repetition Enum</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Profile Repetition Enum</em>'.
	 * @see essim.ProfileRepetitionEnum
	 * @generated
	 */
	EEnum getProfileRepetitionEnum();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	EssimFactory getEssimFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link essim.impl.ESSIMInfluxDBProfileImpl <em>ESSIM Influx DB Profile</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see essim.impl.ESSIMInfluxDBProfileImpl
		 * @see essim.impl.EssimPackageImpl#getESSIMInfluxDBProfile()
		 * @generated
		 */
		EClass ESSIM_INFLUX_DB_PROFILE = eINSTANCE.getESSIMInfluxDBProfile();

		/**
		 * The meta object literal for the '<em><b>Annual Change Percentage</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE = eINSTANCE
				.getESSIMInfluxDBProfile_AnnualChangePercentage();

		/**
		 * The meta object literal for the '<em><b>Start</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ESSIM_INFLUX_DB_PROFILE__START = eINSTANCE.getESSIMInfluxDBProfile_Start();

		/**
		 * The meta object literal for the '<em><b>Profile Repetition</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION = eINSTANCE.getESSIMInfluxDBProfile_ProfileRepetition();

		/**
		 * The meta object literal for the '{@link essim.ESSIMProfile <em>ESSIM Profile</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see essim.ESSIMProfile
		 * @see essim.impl.EssimPackageImpl#getESSIMProfile()
		 * @generated
		 */
		EClass ESSIM_PROFILE = eINSTANCE.getESSIMProfile();

		/**
		 * The meta object literal for the '<em><b>Init Profile</b></em>' operation.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EOperation ESSIM_PROFILE___INIT_PROFILE__DATE_DATE_DURATION = eINSTANCE
				.getESSIMProfile__InitProfile__Date_Date_Duration();

		/**
		 * The meta object literal for the '{@link essim.impl.ESSIMDateTimeProfileImpl <em>ESSIM Date Time Profile</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see essim.impl.ESSIMDateTimeProfileImpl
		 * @see essim.impl.EssimPackageImpl#getESSIMDateTimeProfile()
		 * @generated
		 */
		EClass ESSIM_DATE_TIME_PROFILE = eINSTANCE.getESSIMDateTimeProfile();

		/**
		 * The meta object literal for the '{@link essim.impl.ESSIMSingleValueProfileImpl <em>ESSIM Single Value Profile</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see essim.impl.ESSIMSingleValueProfileImpl
		 * @see essim.impl.EssimPackageImpl#getESSIMSingleValueProfile()
		 * @generated
		 */
		EClass ESSIM_SINGLE_VALUE_PROFILE = eINSTANCE.getESSIMSingleValueProfile();

		/**
		 * The meta object literal for the '{@link essim.ProfileRepetitionEnum <em>Profile Repetition Enum</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see essim.ProfileRepetitionEnum
		 * @see essim.impl.EssimPackageImpl#getProfileRepetitionEnum()
		 * @generated
		 */
		EEnum PROFILE_REPETITION_ENUM = eINSTANCE.getProfileRepetitionEnum();

	}

} //EssimPackage
