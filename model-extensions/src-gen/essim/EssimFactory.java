/**
 */
package essim;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see essim.EssimPackage
 * @generated
 */
public interface EssimFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	EssimFactory eINSTANCE = essim.impl.ESSIMFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>ESSIM Influx DB Profile</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>ESSIM Influx DB Profile</em>'.
	 * @generated
	 */
	ESSIMInfluxDBProfile createESSIMInfluxDBProfile();

	/**
	 * Returns a new object of class '<em>ESSIM Date Time Profile</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>ESSIM Date Time Profile</em>'.
	 * @generated
	 */
	ESSIMDateTimeProfile createESSIMDateTimeProfile();

	/**
	 * Returns a new object of class '<em>ESSIM Single Value Profile</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>ESSIM Single Value Profile</em>'.
	 * @generated
	 */
	ESSIMSingleValueProfile createESSIMSingleValueProfile();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	EssimPackage getEssimPackage();

} //EssimFactory
