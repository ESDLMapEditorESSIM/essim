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
package esdl.impl;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import esdl.EssimESDLFactory;
import esdl.EssimESDLPackage;

/**
 * <!-- begin-user-doc --> An implementation of the model <b>Package</b>. <!-- end-user-doc -->
 * 
 * @generated
 */
public class EssimESDLPackageImpl extends EsdlPackageImpl implements EssimESDLPackage {

	/**
	 * This Package should be used to instantiate the DIDO model, as it specifies dynamic behaviour not available in the
	 * (static) DIDO.ecore specification.
	 * 
	 * 
	 * Creates an instance of the model <b>Package</b>, registered with {@link org.eclipse.emf.ecore.EPackage.Registry
	 * EPackage.Registry} by the package package URI value.
	 * <p>
	 * Note: the correct way to create the package is via the static factory method {@link #init init()}, which also
	 * performs initialization of the package, or returns the registered package, if one already exists. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see dido.DidoPackage#eNS_URI
	 * @see #init()
	 * @generated NOT
	 */
	private EssimESDLPackageImpl() {
		// super(eNS_URI, DidoFactory.eINSTANCE);
		super(eNS_URI, EssimESDLFactory.eINSTANCE);
		System.err.println("Instantiating extended ESSIM ESDL package");
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>
	 * This method is used to initialize {@link DidoPackage#eINSTANCE} when that field is accessed. Clients should not
	 * invoke it directly. Instead, they should simply access that field to obtain the package. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static EssimESDLPackage init() {
		if (isInited)
			return (EssimESDLPackage) EPackage.Registry.INSTANCE.getEPackage(EssimESDLPackage.eNS_URI);

		// Obtain or create and register package
		EssimESDLPackageImpl theDidoPackage = (EssimESDLPackageImpl) (EPackage.Registry.INSTANCE
				.get(eNS_URI) instanceof EssimESDLPackageImpl
						? EPackage.Registry.INSTANCE.get(eNS_URI)
						: new EssimESDLPackageImpl());

		isInited = true;
		//
		// // Initialize simple dependencies
		// DidoESDLPackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theDidoPackage.createPackageContents();

		// Initialize created meta-data
		theDidoPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theDidoPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(EssimESDLPackage.eNS_URI, theDidoPackage);
		return theDidoPackage;
	}

	/**
	 * This method is overridden, so a saved file can find the associated dido.ecore file at the schemaLocation defined
	 * in the XMI (.dido) file
	 */
	@Override
	protected Resource createResource(String uri) {
		String schemaLocation = "../../../esdl/model/esdl.ecore";
		return super.createResource(schemaLocation);
	}

} // DidoPackageImpl
