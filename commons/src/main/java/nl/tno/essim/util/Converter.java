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

package nl.tno.essim.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.ChronoUnit;

import esdl.AbstractQuantityAndUnit;
import esdl.Carrier;
import esdl.DurationUnitEnum;
import esdl.EnergyCarrier;
import esdl.EsdlFactory;
import esdl.Fuel;
import esdl.GenericProfile;
import esdl.MultiplierEnum;
import esdl.ProfileTypeEnum;
import esdl.QuantityAndUnitReference;
import esdl.QuantityAndUnitType;
import esdl.TimeUnitEnum;
import esdl.UnitEnum;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;

/**
 * Contains converter methods between the dido specification (ecore model) and
 * the runtime
 * 
 * @author werkmane
 *
 */
@Slf4j
public class Converter {
	private static NumberFormat formatter = DecimalFormat.getNumberInstance();

	/**
	 * Converts a {@link java.time.Duration} to {@link esdl.Duration}<br>
	 * It uses {@link DurationUnitEnum#SECOND} as duration Unit as
	 * java.time.Duration is defined in seconds.
	 * 
	 * @param duration in java.time version
	 * @return esdl.Duration using SECOND as duration unit
	 */
	public static esdl.Duration toESDLDuration(java.time.Duration duration) {
		esdl.Duration esdlDuration = EsdlFactory.eINSTANCE.createDuration();
		esdlDuration.setDurationUnit(DurationUnitEnum.SECOND);
		esdlDuration.setValue(duration.getSeconds());
		return esdlDuration;
	}

	public static esdl.Duration toESDLDuration(EssimDuration duration) {
		esdl.Duration esdlDuration = EsdlFactory.eINSTANCE.createDuration();
		// TODO: BAD HACK! test if this works: parse based on removing the last 'S' from
		// one enum to the other
		esdlDuration.setDurationUnit(DurationUnitEnum
				.valueOf(duration.getUnit().name().substring(0, duration.getUnit().name().length() - 1)));
		esdlDuration.setValue(duration.getAmount());
		return esdlDuration;
	}

	public static EssimDuration toEssimDuration(esdl.Duration duration) {
		return EssimDuration.of(duration.getValue(), ChronoUnit.valueOf(duration.getDurationUnit().getName() + "S"));
	}

	public static double toStandardizedUnits(double amount, Object unit) {
		if (unit instanceof ProfileTypeEnum) {
			return profileTypeToStandardizedUnits(amount, (ProfileTypeEnum) unit);
		} else if (unit instanceof AbstractQuantityAndUnit) {
			return abstractQuantityToStandardizedUnits(amount, (AbstractQuantityAndUnit) unit);
		} else {
			return amount;
		}
	}

	/**
	 * Converts the unit into DIDO's default units (Joule for energy, W for Power,
	 * Celcius for Temperature)
	 * 
	 * @param amount the amount in 'unit'
	 * @param unit   of the amount
	 * @return the amount in the DIDO unit
	 */
	private static double profileTypeToStandardizedUnits(double amount, ProfileTypeEnum unit) {
		switch (unit) {
		case ENERGY_IN_GJ:
			return amount * 1E9;
		case ENERGY_IN_GWH:
			return amount * 3.6E12;
		case ENERGY_IN_J:
			return amount;
		case ENERGY_IN_KJ:
			return amount * 1000;
		case ENERGY_IN_KWH:
			return amount * 3.6E6;
		case ENERGY_IN_MJ:
			return amount * 1E6;
		case ENERGY_IN_MWH:
			return amount * 3.6E9;
		case ENERGY_IN_PJ:
			return amount * 1E15;
		case ENERGY_IN_TJ:
			return amount * 1E12;
		case ENERGY_IN_WH:
			return amount * 3.6E3;
		case POWER_IN_GW:
			return amount * 1E9;
		case POWER_IN_KW:
			return amount * 1E3;
		case POWER_IN_MW:
			return amount * 1E6;
		case POWER_IN_TW:
			return amount * 1E12;
		case POWER_IN_W:
			return amount;
		case TEMPERATURE_IN_K:
			return amount + 273.15;
		case TEMPERATURE_IN_C:
		case STATEOFCHARGE_IN_WS:
		case SOLARIRRADIANCE_IN_WPER_M2:
		case WINDSPEED_IN_MPER_S:
		default:
			return amount;
		}
	}

	/**
	 * Converts the given amount to the amount in the given unit
	 * 
	 * @param amount
	 * @param unit
	 * @return the converted amount
	 */
	public static double fromDidoUnit(double amount, ProfileTypeEnum unit) {
		switch (unit) {
		case ENERGY_IN_GJ:
			return amount / 1E9;
		case ENERGY_IN_GWH:
			return amount / 3.6E12;
		case ENERGY_IN_J:
			return amount;
		case ENERGY_IN_KJ:
			return amount / 1000;
		case ENERGY_IN_KWH:
			return amount / 3.6E6;
		case ENERGY_IN_MJ:
			return amount / 1E6;
		case ENERGY_IN_MWH:
			return amount / 3.6E9;
		case ENERGY_IN_PJ:
			return amount / 1E15;
		case ENERGY_IN_TJ:
			return amount / 1E12;
		case ENERGY_IN_WH:
			return amount / 3.6E3;
		case POWER_IN_GW:
			return amount / 1E9;
		case POWER_IN_KW:
			return amount / 1E3;
		case POWER_IN_MW:
			return amount / 1E6;
		case POWER_IN_TW:
			return amount / 1E12;
		case POWER_IN_W:
			return amount;
		case TEMPERATURE_IN_K:
			return amount - 273.15;
		case TEMPERATURE_IN_C:
		case STATEOFCHARGE_IN_WS:
		case SOLARIRRADIANCE_IN_WPER_M2:
		case WINDSPEED_IN_MPER_S:
		default:
			return amount;
		}
	}

	/**
	 * Formats a number in a specific unit using the current Locale
	 * 
	 * @param value
	 * @param unit
	 * @return
	 */
	public static String format(double value, ProfileTypeEnum unit) {
		return formatter.format(fromDidoUnit(value, unit));
	}
	
	public static QuantityAndUnitType resolveAbstractQuantityAndUnit(AbstractQuantityAndUnit specifiedUnit) {
		if (specifiedUnit instanceof QuantityAndUnitType) {
			return (QuantityAndUnitType) specifiedUnit;
		} else if (specifiedUnit instanceof QuantityAndUnitReference) {
			QuantityAndUnitReference reference = (QuantityAndUnitReference) specifiedUnit;
			return reference.getReference();
		} else {
			throw new IllegalArgumentException("Unknown AbstractQuantityAndUnit " + specifiedUnit);
		}
	}

	public static double abstractQuantityToStandardizedUnits(double value, AbstractQuantityAndUnit specifiedUnit) {
		QuantityAndUnitType standardizedUnit = resolveAbstractQuantityAndUnit(specifiedUnit); 

		if (standardizedUnit == null) {
			GenericProfile profile = (GenericProfile) specifiedUnit.eContainer();
			throw new IllegalArgumentException("Quantity and Unit is missing for profile with id : " + profile.getId());
		}
		
		// find out the energy content of the carrier (Fuel or EnergyCarrier)
		// TODO: check if this is generic enough or that more checks are necessary than the perUnit check.
		double energyContentFactor = 1;
		if (standardizedUnit.eContainer() instanceof GenericProfile ) {
			GenericProfile genericProfile = (GenericProfile) standardizedUnit.eContainer();
			if (genericProfile.eContainer() != null && genericProfile.eContainer() instanceof Carrier) {
				Carrier carrier = (Carrier) genericProfile.eContainer();
				if (carrier instanceof Fuel) {
					Fuel fuel = (Fuel) carrier;
					double energyContent = fuel.getEnergyContent();
					QuantityAndUnitType energyContentUnit = resolveAbstractQuantityAndUnit(fuel.getEnergyContentUnit());
					// check if perUnit of e.g. MJ/m3 (==m3) is the same as the input unit e.g. EUR/m3 (==m3)
					if (standardizedUnit.getPerUnit() == energyContentUnit.getPerUnit()) {
						energyContentFactor = abstractQuantityToStandardizedUnits(energyContent, energyContentUnit);
						//log.debug("Factor for fuel " + carrier.getName() + "=" + energyContentFactor);
					} else {
						String error = String.format("Incompatible units: Cannot convert energyContent unit %s/%s of %s %s such that it is usable for profile %s defined in %s/%s", 
								energyContentUnit.getUnit(), energyContentUnit.getPerUnit(), fuel.eClass().getName(), fuel.getName(), genericProfile.getName(), standardizedUnit.getUnit(),standardizedUnit.getPerUnit());
						log.error(error);
						throw new IllegalArgumentException(error);
						
					}
				} else if (carrier instanceof EnergyCarrier) {
					EnergyCarrier energyCarrier = (EnergyCarrier) carrier;
					double energyContent = energyCarrier.getEnergyContent();
					QuantityAndUnitType energyContentUnit = resolveAbstractQuantityAndUnit(energyCarrier.getEnergyContentUnit());
					if (standardizedUnit.getPerUnit() == energyContentUnit.getPerUnit()) {
						energyContentFactor = abstractQuantityToStandardizedUnits(energyContent, energyContentUnit);
						//log.debug("Factor for energy carrier " + carrier + "=" + energyContentFactor);
					} else {
						String error = String.format("Incompatible units: Cannot convert energyContent unit %s/%s of %s %s such that it is usable for profile %s defined in %s/%s", 
								energyContentUnit.getUnit(), energyContentUnit.getPerUnit(), energyCarrier.eClass().getName(), energyCarrier.getName(), genericProfile.getName(), standardizedUnit.getUnit(),standardizedUnit.getPerUnit());
						log.error(error);
						throw new IllegalArgumentException(error);
					}
				}
				
			}
			 
		}		

		return value * (fromMultiplier(standardizedUnit.getMultiplier()) * toSIUnit(standardizedUnit.getUnit()))
				/ (fromMultiplier(standardizedUnit.getPerMultiplier()) * toSIUnit(standardizedUnit.getPerUnit())
						* toSIUnit(standardizedUnit.getPerTimeUnit())) / energyContentFactor;
	}

	private static double toSIUnit(TimeUnitEnum unit) {
		if (unit == null) {
			return 1;
		}
		// Convert all to seconds
		switch (unit) {
		case DAY:
			return 24 * 60 * 60;
		case HOUR:
			return 60 * 60;
		case MINUTE:
			return 60;
		case MONTH:
			return 30 * 24 * 60 * 60;
		case NONE:
			return 1;
		case QUARTER:
			return 15 * 60;
		case SECOND:
			return 1;
		case WEEK:
			return 7 * 24 * 60 * 60;
		case YEAR:
			return 365 * 24 * 60 * 60;
		default:
			return 1;
		}
	}

	private static double toSIUnit(UnitEnum unit) {
		if (unit == null) {
			return 1;
		}
		switch (unit) {
		case ARE:
			return 100; // m2
		case BAR:
			return 100000; // Pa
		case CUBIC_METRE:
			return 1; // m3
		case DAY:
			return 24 * 60 * 60; // sec
		case DEGREES_CELSIUS:
			return 1;
		case DOLLAR:
			return 1;
		case EURO:
			return 1;
		case GRAM:
			return 1e-3; // kg
		case HECTARE:
			return 1e4; // m2
		case HOUR:
			return 60 * 60; // sec
		case JOULE:
			return 1;
		case KELVIN:
			return 1;
		case LITRE:
			return 1e-3; // m3
		case METRE:
			return 1; // m
		case MINUTE:
			return 60; // sec
		case MONTH:
			return 30 * 24 * 60 * 60; // sec
		case NONE:
			return 1;
		case PERCENT:
			return 1;
		case PSI:
			return 6894.76; // Pa
		case QUARTER:
			return 15 * 60; // sec
		case SECOND:
			return 1;
		case SQUARE_METRE:
			return 1;
		case VOLT:
			return 1;
		case WATT:			
			return 1;
		case WATTHOUR:
			return 3600; // J (Ws)
		case WATTSECOND:
			return 1; // J
		case WEEK:
			return 7 * 24 * 60 * 60; // sec
		case YEAR:
			return 365 * 24 * 60 * 60; // sec
		case AMPERE:
		case DEGREES:
		case NEWTON:
		case PASCAL:
		case VOLT_AMPERE:
		case VOLT_AMPERE_REACTIVE:
			return 1;
		default:
			return 1;
		}
	}

	private static double fromMultiplier(MultiplierEnum multiplier) {
		if (multiplier == null) {
			return 1;
		}
		switch (multiplier) {
		case GIGA:
			return 1e9;
		case KILO:
			return 1e3;
		case MEGA:
			return 1e6;
		case MICRO:
			return 1e-6;
		case MILLI:
			return 1e-3;
		case NANO:
			return 1e-9;
		case NONE:
			return 1;
		case PETA:
			return 1e15;
		case PICO:
			return 1e-12;
		case TERA:
		case TERRA:
			return 1e12;
		default:
			return 1;
		}
	}

}