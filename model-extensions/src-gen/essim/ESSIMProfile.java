/**
 */
package essim;

import esdl.Duration;

import java.util.Date;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ESSIM Profile</b></em>'.
 * <!-- end-user-doc -->
 *
 *
 * @see essim.EssimPackage#getESSIMProfile()
 * @model interface="true" abstract="true"
 * @generated
 */
public interface ESSIMProfile extends EObject {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model
	 * @generated
	 */
	void initProfile(Date from, Date to, Duration aggregationPrecision);

} // ESSIMProfile
