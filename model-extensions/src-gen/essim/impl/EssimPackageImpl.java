/**
 */
package essim.impl;

import esdl.EsdlPackage;

import esdl.impl.EsdlPackageImpl;

import essim.ESSIMDateTimeProfile;
import essim.ESSIMInfluxDBProfile;
import essim.ESSIMProfile;
import essim.ESSIMSingleValueProfile;
import essim.EssimFactory;
import essim.EssimPackage;
import essim.ExtendedESSIMFactory;
import essim.ProfileRepetitionEnum;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class EssimPackageImpl extends EPackageImpl implements EssimPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass essimInfluxDBProfileEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass essimProfileEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass essimDateTimeProfileEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass essimSingleValueProfileEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum profileRepetitionEnumEEnum = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see essim.EssimPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private EssimPackageImpl() {
		super(eNS_URI, EssimFactory.eINSTANCE);
	}

	public EssimPackageImpl(String ensUri, ExtendedESSIMFactory einstance) {
		super(eNS_URI, einstance);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link EssimPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static EssimPackage init() {
		if (isInited)
			return (EssimPackage) EPackage.Registry.INSTANCE.getEPackage(EssimPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredEssimPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		EssimPackageImpl theEssimPackage = registeredEssimPackage instanceof EssimPackageImpl
				? (EssimPackageImpl) registeredEssimPackage
				: new EssimPackageImpl();

		isInited = true;

		// Obtain or create and register interdependencies
		Object registeredPackage = EPackage.Registry.INSTANCE.getEPackage(EsdlPackage.eNS_URI);
		EsdlPackageImpl theEsdlPackage = (EsdlPackageImpl) (registeredPackage instanceof EsdlPackageImpl
				? registeredPackage
				: EsdlPackage.eINSTANCE);

		// Create package meta-data objects
		theEssimPackage.createPackageContents();
		theEsdlPackage.createPackageContents();

		// Initialize created meta-data
		theEssimPackage.initializePackageContents();
		theEsdlPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theEssimPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(EssimPackage.eNS_URI, theEssimPackage);
		return theEssimPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getESSIMInfluxDBProfile() {
		return essimInfluxDBProfileEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getESSIMInfluxDBProfile_AnnualChangePercentage() {
		return (EAttribute) essimInfluxDBProfileEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getESSIMInfluxDBProfile_Start() {
		return (EAttribute) essimInfluxDBProfileEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getESSIMInfluxDBProfile_ProfileRepetition() {
		return (EAttribute) essimInfluxDBProfileEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getESSIMProfile() {
		return essimProfileEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EOperation getESSIMProfile__InitProfile__Date_Date_Duration() {
		return essimProfileEClass.getEOperations().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getESSIMDateTimeProfile() {
		return essimDateTimeProfileEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getESSIMSingleValueProfile() {
		return essimSingleValueProfileEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getProfileRepetitionEnum() {
		return profileRepetitionEnumEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EssimFactory getEssimFactory() {
		return (EssimFactory) getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated)
			return;
		isCreated = true;

		// Create classes and their features
		essimInfluxDBProfileEClass = createEClass(ESSIM_INFLUX_DB_PROFILE);
		createEAttribute(essimInfluxDBProfileEClass, ESSIM_INFLUX_DB_PROFILE__ANNUAL_CHANGE_PERCENTAGE);
		createEAttribute(essimInfluxDBProfileEClass, ESSIM_INFLUX_DB_PROFILE__START);
		createEAttribute(essimInfluxDBProfileEClass, ESSIM_INFLUX_DB_PROFILE__PROFILE_REPETITION);

		essimProfileEClass = createEClass(ESSIM_PROFILE);
		createEOperation(essimProfileEClass, ESSIM_PROFILE___INIT_PROFILE__DATE_DATE_DURATION);

		essimDateTimeProfileEClass = createEClass(ESSIM_DATE_TIME_PROFILE);

		essimSingleValueProfileEClass = createEClass(ESSIM_SINGLE_VALUE_PROFILE);

		// Create enums
		profileRepetitionEnumEEnum = createEEnum(PROFILE_REPETITION_ENUM);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized)
			return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		EsdlPackage theEsdlPackage = (EsdlPackage) EPackage.Registry.INSTANCE.getEPackage(EsdlPackage.eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		essimInfluxDBProfileEClass.getESuperTypes().add(theEsdlPackage.getInfluxDBProfile());
		essimInfluxDBProfileEClass.getESuperTypes().add(this.getESSIMProfile());
		essimDateTimeProfileEClass.getESuperTypes().add(theEsdlPackage.getDateTimeProfile());
		essimDateTimeProfileEClass.getESuperTypes().add(this.getESSIMProfile());
		essimSingleValueProfileEClass.getESuperTypes().add(theEsdlPackage.getSingleValue());
		essimSingleValueProfileEClass.getESuperTypes().add(this.getESSIMProfile());

		// Initialize classes, features, and operations; add parameters
		initEClass(essimInfluxDBProfileEClass, ESSIMInfluxDBProfile.class, "ESSIMInfluxDBProfile", !IS_ABSTRACT,
				!IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getESSIMInfluxDBProfile_AnnualChangePercentage(), ecorePackage.getEDouble(),
				"annualChangePercentage", null, 0, 1, ESSIMInfluxDBProfile.class, !IS_TRANSIENT, !IS_VOLATILE,
				IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getESSIMInfluxDBProfile_Start(), ecorePackage.getEDate(), "start", null, 1, 1,
				ESSIMInfluxDBProfile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID,
				IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getESSIMInfluxDBProfile_ProfileRepetition(), this.getProfileRepetitionEnum(),
				"profileRepetition", null, 0, 1, ESSIMInfluxDBProfile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE,
				!IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(essimProfileEClass, ESSIMProfile.class, "ESSIMProfile", IS_ABSTRACT, IS_INTERFACE,
				IS_GENERATED_INSTANCE_CLASS);

		EOperation op = initEOperation(getESSIMProfile__InitProfile__Date_Date_Duration(), null, "initProfile", 0, 1,
				IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEDate(), "from", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, ecorePackage.getEDate(), "to", 0, 1, IS_UNIQUE, IS_ORDERED);
		addEParameter(op, theEsdlPackage.getDuration(), "aggregationPrecision", 0, 1, IS_UNIQUE, IS_ORDERED);

		initEClass(essimDateTimeProfileEClass, ESSIMDateTimeProfile.class, "ESSIMDateTimeProfile", !IS_ABSTRACT,
				!IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(essimSingleValueProfileEClass, ESSIMSingleValueProfile.class, "ESSIMSingleValueProfile",
				!IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		// Initialize enums and add enum literals
		initEEnum(profileRepetitionEnumEEnum, ProfileRepetitionEnum.class, "ProfileRepetitionEnum");
		addEEnumLiteral(profileRepetitionEnumEEnum, ProfileRepetitionEnum.NONE);
		addEEnumLiteral(profileRepetitionEnumEEnum, ProfileRepetitionEnum.WEEKLY);
		addEEnumLiteral(profileRepetitionEnumEEnum, ProfileRepetitionEnum.YEARLY);

		// Create resource
		createResource(eNS_URI);
	}

} //EssimPackageImpl
