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

import esdl.DatabaseProfile;
import esdl.DateTimeProfile;
import esdl.ExternalProfile;
import esdl.GenericProfile;
import esdl.InfluxDBProfile;

import esdl.SingleValue;
import esdl.StaticProfile;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see essim.EssimPackage
 * @generated
 */
public class EssimAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static EssimPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EssimAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = EssimPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject) object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EssimSwitch<Adapter> modelSwitch = new EssimSwitch<Adapter>() {
		@Override
		public Adapter caseESSIMInfluxDBProfile(ESSIMInfluxDBProfile object) {
			return createESSIMInfluxDBProfileAdapter();
		}

		@Override
		public Adapter caseESSIMProfile(ESSIMProfile object) {
			return createESSIMProfileAdapter();
		}

		@Override
		public Adapter caseESSIMDateTimeProfile(ESSIMDateTimeProfile object) {
			return createESSIMDateTimeProfileAdapter();
		}

		@Override
		public Adapter caseESSIMSingleValueProfile(ESSIMSingleValueProfile object) {
			return createESSIMSingleValueProfileAdapter();
		}

		@Override
		public Adapter caseGenericProfile(GenericProfile object) {
			return createGenericProfileAdapter();
		}

		@Override
		public Adapter caseExternalProfile(ExternalProfile object) {
			return createExternalProfileAdapter();
		}

		@Override
		public Adapter caseDatabaseProfile(DatabaseProfile object) {
			return createDatabaseProfileAdapter();
		}

		@Override
		public Adapter caseInfluxDBProfile(InfluxDBProfile object) {
			return createInfluxDBProfileAdapter();
		}

		@Override
		public Adapter caseStaticProfile(StaticProfile object) {
			return createStaticProfileAdapter();
		}

		@Override
		public Adapter caseDateTimeProfile(DateTimeProfile object) {
			return createDateTimeProfileAdapter();
		}

		@Override
		public Adapter caseSingleValue(SingleValue object) {
			return createSingleValueAdapter();
		}

		@Override
		public Adapter defaultCase(EObject object) {
			return createEObjectAdapter();
		}
	};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject) target);
	}

	/**
	 * Creates a new adapter for an object of class '{@link essim.ESSIMInfluxDBProfile <em>ESSIM Influx DB Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see essim.ESSIMInfluxDBProfile
	 * @generated
	 */
	public Adapter createESSIMInfluxDBProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link essim.ESSIMProfile <em>ESSIM Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see essim.ESSIMProfile
	 * @generated
	 */
	public Adapter createESSIMProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link essim.ESSIMDateTimeProfile <em>ESSIM Date Time Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see essim.ESSIMDateTimeProfile
	 * @generated
	 */
	public Adapter createESSIMDateTimeProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link essim.ESSIMSingleValueProfile <em>ESSIM Single Value Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see essim.ESSIMSingleValueProfile
	 * @generated
	 */
	public Adapter createESSIMSingleValueProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.GenericProfile <em>Generic Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.GenericProfile
	 * @generated
	 */
	public Adapter createGenericProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.ExternalProfile <em>External Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.ExternalProfile
	 * @generated
	 */
	public Adapter createExternalProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.DatabaseProfile <em>Database Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.DatabaseProfile
	 * @generated
	 */
	public Adapter createDatabaseProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.InfluxDBProfile <em>Influx DB Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.InfluxDBProfile
	 * @generated
	 */
	public Adapter createInfluxDBProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.StaticProfile <em>Static Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.StaticProfile
	 * @generated
	 */
	public Adapter createStaticProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.DateTimeProfile <em>Date Time Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.DateTimeProfile
	 * @generated
	 */
	public Adapter createDateTimeProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link esdl.SingleValue <em>Single Value</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see esdl.SingleValue
	 * @generated
	 */
	public Adapter createSingleValueAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //EssimAdapterFactory
