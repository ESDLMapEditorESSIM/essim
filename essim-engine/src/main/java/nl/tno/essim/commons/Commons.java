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

package nl.tno.essim.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;

import com.google.common.collect.RangeMap;

import esdl.AbstractBasicConversion;
import esdl.AbstractQuantityAndUnit;
import esdl.Carrier;
import esdl.Consumer;
import esdl.Conversion;
import esdl.EnergyAsset;
import esdl.EssimESDLFactory;
import esdl.GenericProfile;
import esdl.InPort;
import esdl.MultiplierEnum;
import esdl.OutPort;
import esdl.PhysicalQuantityEnum;
import esdl.Port;
import esdl.Producer;
import esdl.ProfileElement;
import esdl.ProfileReference;
import esdl.ProfileTypeEnum;
import esdl.QuantityAndUnitReference;
import esdl.QuantityAndUnitType;
import esdl.TimeUnitEnum;
import esdl.UnitEnum;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.time.Horizon;
import nl.tno.essim.util.Converter;

public class Commons {
	public static final double eps = 1e-5;
	public static final double DEFAULT_EFFICIENCY = 0.6;
	public static final double DEFAULT_COGEN_H_EFF = 0.35;
	public static final double DEFAULT_COGEN_E_EFF = 0.55;
	public static final double DEFAULT_HP_COP = 3.5;
	public static final double P_MIN = 0.0;
	public static final double P_MAX = 1.0;
	public static final String RESOURCE = ".";
	public static RangeMap<Double, String> thresholdMap;
	private static HashMap<Port, GenericProfile> portProfileMap = new HashMap<Port, GenericProfile>();

	public static enum Role {
		TRANSPORT, PRODUCER, CONSUMER, BOTH
	};

	public static String readFileIntoString(String filename) throws IOException {
		try (InputStream is = Commons.class.getClassLoader().getResourceAsStream(filename)) {
			return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
		}
	}

	public static boolean isConversionToSameCarrier(EnergyAsset asset) {
		List<Carrier> carrierList = new ArrayList<Carrier>();
		if (asset instanceof Conversion) {
			for (Port port : asset.getPort()) {
				Carrier carrier = port.getCarrier();
				if (!carrierList.contains(carrier)) {
					carrierList.add(carrier);
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public static List<Double> readProfile(Port port, Horizon horizon) {
		return readProfile(getEnergyProfile(port), port, horizon);
	}

	public static GenericProfile getEnergyProfile(Port port) {
		if (!portProfileMap.containsKey(port)) {
			for (GenericProfile profile : port.getProfile()) {
				if (profile instanceof ProfileReference) {
					profile = ((ProfileReference) profile).getReference();
				}
				if (isEnergyProfile(profile) || isPowerProfile(profile) || isPercentageProfile(profile)) {
					portProfileMap.put(port, profile);
					return profile;
				}
			}
		}
		GenericProfile energyProfile = portProfileMap.get(port);
		return energyProfile;
	}

	public static List<Double> readProfile(GenericProfile profile, Port port, Horizon horizon) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		if (profile != null) {
			Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
			Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

			EnergyAsset energyAsset = port.getEnergyasset();
			double ratedPower = 1.0;
			if (energyAsset instanceof Producer) {
				Producer producer = (Producer) energyAsset;
				ratedPower = producer.getPower();
			} else if (energyAsset instanceof Consumer) {
				Consumer consumer = (Consumer) energyAsset;
				ratedPower = consumer.getPower();
			} else if (energyAsset instanceof AbstractBasicConversion) {
				AbstractBasicConversion conversion = (AbstractBasicConversion) energyAsset;
				ratedPower = conversion.getPower();
			}
			if (!isPercentageProfile(profile)) {
				ratedPower = 1.0;
			}
			final double power = ratedPower;

			EList<ProfileElement> profileElements = profile.getProfile(from, to,
					Converter.toESDLDuration(horizon.getPeriod()));
			return profileElements.stream().mapToDouble(s -> s.getValue() * power).boxed().collect(Collectors.toList());
		}
		return null;
	}

	public static List<Double> readProfile(GenericProfile profile, Horizon horizon) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		if (profile != null) {
			Date from = EssimTime.localDateTimeToDate(horizon.getStartTime());
			Date to = EssimTime.localDateTimeToDate(horizon.getEndTime());

			EList<ProfileElement> profileElements = profile.getProfile(from, to,
					Converter.toESDLDuration(horizon.getPeriod()));
			return profileElements.stream().mapToDouble(s -> s.getValue()).boxed().collect(Collectors.toList());
		}
		return null;
	}

	public static void writeProfile(Port port, EssimTime timestamp, double value) {
		EList<GenericProfile> profiles = port.getProfile();
		GenericProfile updatedProfile;

		if (profiles.isEmpty()) {
			GenericProfile profile = null;
			updatedProfile = writeProfile(profile, timestamp, value);
		} else {
			GenericProfile profile = profiles.get(0);
			if (profile instanceof ProfileReference) {
				profile = ((ProfileReference) profile).getReference();
			}
			updatedProfile = writeProfile(profile, timestamp, value);
		}
		profiles.add(updatedProfile);
	}

	public static GenericProfile writeProfile(GenericProfile profile, EssimTime timestamp, double value) {
		if (profile == null) {
			profile = EssimESDLFactory.eINSTANCE.createDateTimeProfile();
		}

		EList<ProfileElement> profileElementList = ECollections.newBasicEList();
		ProfileElement element = EssimESDLFactory.eINSTANCE.createProfileElement();
		element.setValue(value);
		element.setFrom(EssimTime.localDateTimeToDate(timestamp.getTime()));

		profileElementList.add(element);
		profile.setProfile(profileElementList);
		profile.setProfileType(ProfileTypeEnum.ENERGY_IN_J);

		return profile;
	}

	public static Double aggregateCost(List<Double> costProfile) {
		return averageOrNothing(costProfile);
	}

	public static Double sum(List<Double> energyProfile) {
		return sumOrNothing(energyProfile);
	}

	public static Double aggregateEnergy(List<Double> energyProfile) {
		return sumOrNothing(energyProfile);
	}

	public static Double aggregateGas(List<Double> gasProfile) {
		return sumOrNothing(gasProfile);
	}

	public static Double aggregatePower(List<Double> energyProfile) {
		return averageOrNothing(energyProfile);
	}

	public static Double aggregateSoC(List<Double> socProfile) {
		return averageOrNothing(socProfile);
	}

	private static Double sumOrNothing(List<Double> profile) {
		if (profile == null || profile.isEmpty()) {
			return Double.NaN;
		} else {
			return profile.stream().mapToDouble(fn -> fn.doubleValue()).sum();
		}
	}

	private static Double averageOrNothing(List<Double> profile) {
		if (profile == null || profile.isEmpty()) {
			return Double.NaN;
		} else {
			return profile.stream().mapToDouble(fn -> fn.doubleValue()).average().orElse(0.0);
		}
	}

	public static boolean isPowerProfile(GenericProfile profile) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
		if (profileQandU != null) {
			QuantityAndUnitType qu = null;
			if (profileQandU instanceof QuantityAndUnitType) {
				qu = (QuantityAndUnitType) profileQandU;
			} else if (profileQandU instanceof QuantityAndUnitReference) {
				QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
				qu = reference.getReference();
			}
			return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.POWER);
		}

		return (profile.getProfileType().getValue() <= ProfileTypeEnum.POWER_IN_TW_VALUE)
				&& (profile.getProfileType().getValue() >= ProfileTypeEnum.POWER_IN_W_VALUE);
	}

	public static boolean isEnergyProfile(GenericProfile profile) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
		if (profileQandU != null) {
			QuantityAndUnitType qu = null;
			if (profileQandU instanceof QuantityAndUnitType) {
				qu = (QuantityAndUnitType) profileQandU;
			} else if (profileQandU instanceof QuantityAndUnitReference) {
				QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
				qu = reference.getReference();
			}
			return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.ENERGY);
		}

		return (profile.getProfileType().getValue() <= ProfileTypeEnum.ENERGY_IN_PJ_VALUE)
				&& (profile.getProfileType().getValue() >= ProfileTypeEnum.ENERGY_IN_WH_VALUE);
	}

	public static boolean isPercentageProfile(GenericProfile profile) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
		if (profileQandU != null) {
			QuantityAndUnitType qu = null;
			if (profileQandU instanceof QuantityAndUnitType) {
				qu = (QuantityAndUnitType) profileQandU;
			} else if (profileQandU instanceof QuantityAndUnitReference) {
				QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
				qu = reference.getReference();
			}
			return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.COEFFICIENT);
		}

		return (profile.getProfileType().getValue() == ProfileTypeEnum.PERCENTAGE_VALUE);
	}

	public static boolean isSoCProfile(GenericProfile profile) {
		if (profile instanceof ProfileReference) {
			profile = ((ProfileReference) profile).getReference();
		}
		AbstractQuantityAndUnit profileQandU = profile.getProfileQuantityAndUnit();
		if (profileQandU != null) {
			QuantityAndUnitType qu = null;
			if (profileQandU instanceof QuantityAndUnitType) {
				qu = (QuantityAndUnitType) profileQandU;
			} else if (profileQandU instanceof QuantityAndUnitReference) {
				QuantityAndUnitReference reference = (QuantityAndUnitReference) profileQandU;
				qu = reference.getReference();
			}
			return qu != null && qu.getPhysicalQuantity().equals(PhysicalQuantityEnum.STATE_OF_CHARGE);
		}

		return profile.getProfileType().equals(ProfileTypeEnum.STATEOFCHARGE_IN_WS);
	}

	public static HashMap<EnergyAsset, Port> findAllConnectedAssets(EnergyAsset asset) {
		HashMap<EnergyAsset, Port> connectedAssets = new HashMap<EnergyAsset, Port>();

		for (Port port : asset.getPort()) {
			if (port instanceof InPort) {
				InPort inPort = (InPort) port;
				for (OutPort outPort : inPort.getConnectedTo()) {
					connectedAssets.put(outPort.getEnergyasset(), outPort);
				}
			} else if (port instanceof OutPort) {
				OutPort outPort = (OutPort) port;
				for (InPort inPort : outPort.getConnectedTo()) {
					connectedAssets.put(inPort.getEnergyasset(), inPort);
				}
			} else {
				throw new IllegalStateException("Port type " + port + " is not supported");
			}
		}

		return connectedAssets;
	}

	public static double toStandardizedUnits(double value, AbstractQuantityAndUnit specifiedUnit) {
		QuantityAndUnitType standardizedUnit;
		if (specifiedUnit instanceof QuantityAndUnitType) {
			standardizedUnit = (QuantityAndUnitType) specifiedUnit;
		} else if (specifiedUnit instanceof QuantityAndUnitReference) {
			QuantityAndUnitReference reference = (QuantityAndUnitReference) specifiedUnit;
			standardizedUnit = reference.getReference();
		} else {
			return value;
		}

		return value * (fromMultiplier(standardizedUnit.getMultiplier()) * toSIUnit(standardizedUnit.getUnit()))
				/ (fromMultiplier(standardizedUnit.getPerMultiplier()) * toSIUnit(standardizedUnit.getPerUnit())
						* toSIUnit(standardizedUnit.getPerTimeUnit()));
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
			return 0.01;
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
		case TERRA:
			return 1e12;
		default:
			return 1;
		}
	}

	public static boolean isNumericString(String str) {
		return str.matches("-?\\d+(\\.\\d+)?"); // match a number with optional '-' and decimal.
	}

}
