/**
 */
package nl.tno.essim.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Simulation Status</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see dido.DidoPackage#getSimulationStatus()
 * @model
 * @generated
 */
public enum SimulationStatus {
	UNDEFINED(0, "UNDEFINED", "UNDEFINED"),
	SETUP(1, "SETUP", "SETUP"),
	STARTED(2, "STARTED", "STARTED"),
	PAUSED(3, "PAUSED", "PAUSED"),
	STOPPED(4, "STOPPED", "STOPPED"),
	FINISHED(5, "FINISHED", "FINISHED"),
	ERROR(6, "ERROR", "ERROR");
	
	public static final int UNDEFINED_VALUE = 0;
	public static final int SETUP_VALUE = 1;
	public static final int STARTED_VALUE = 2;
	public static final int PAUSED_VALUE = 3;
	public static final int STOPPED_VALUE = 4;
	public static final int FINISHED_VALUE = 5;
	public static final int ERROR_VALUE = 6;

	private static final SimulationStatus[] VALUES_ARRAY =
		new SimulationStatus[] {
			UNDEFINED,
			SETUP,
			STARTED,
			PAUSED,
			STOPPED,
			FINISHED,
			ERROR,
		};

	/**
	 * A public read-only list of all the '<em><b>Simulation Status</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<SimulationStatus> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Simulation Status</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SimulationStatus get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			SimulationStatus result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Simulation Status</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SimulationStatus getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			SimulationStatus result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Simulation Status</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static SimulationStatus get(int value) {
		switch (value) {
			case UNDEFINED_VALUE: return UNDEFINED;
			case SETUP_VALUE: return SETUP;
			case STARTED_VALUE: return STARTED;
			case PAUSED_VALUE: return PAUSED;
			case STOPPED_VALUE: return STOPPED;
			case FINISHED_VALUE: return FINISHED;
			case ERROR_VALUE: return ERROR;
		}
		return null;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final int value;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String name;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String literal;

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private SimulationStatus(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getValue() {
	  return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
	  return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLiteral() {
	  return literal;
	}

	/**
	 * Returns the literal value of the enumerator, which is its string representation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		return literal;
	}
	
} //SimulationStatus
