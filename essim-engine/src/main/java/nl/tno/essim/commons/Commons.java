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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import esdl.AbstractQuantityAndUnit;
import esdl.Carrier;
import esdl.Conversion;
import esdl.EnergyAsset;
import esdl.InPort;
import esdl.MultiplierEnum;
import esdl.OutPort;
import esdl.Port;
import esdl.QuantityAndUnitReference;
import esdl.QuantityAndUnitType;
import esdl.TimeUnitEnum;
import esdl.UnitEnum;

public class Commons {
	public static final double eps = 1e-5;
	public static final double DEFAULT_EFFICIENCY = 0.6;
	public static final double DEFAULT_COGEN_H_EFF = 0.35;
	public static final double DEFAULT_COGEN_E_EFF = 0.55;
	public static final double DEFAULT_HP_COP = 3.5;
	public static final double P_MIN = 0.0;
	public static final double MIN_DELTA_PRICE = 1E-6;
	public static final double P_MAX = 1.0;
	public static final String RESOURCE = ".";
	public static final String ESSIM_REVISION = "ESSIM_REVISION";

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

	/**
	 * List directory contents for a resource folder. Not recursive. This is
	 * basically a brute-force implementation. Works for regular files and also
	 * JARs.
	 * 
	 * @author Greg Briggs
	 * @param clazz Any java class that lives in the same place as the resources you
	 *              want.
	 * @param path  Should end with "/", but not start with one.
	 * @return Just the name of each member item, not the full paths.
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
		URL dirURL = clazz.getClassLoader().getResource(path);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
			/* A file path: easy enough */
			return new File(dirURL.toURI()).list();
		}

		if (dirURL == null) {
			/*
			 * In case of a jar file, we can't actually find a directory. Have to assume the
			 * same jar as clazz.
			 */
			String me = clazz.getName().replace(".", "/") + ".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL.getProtocol().equals("jar")) {
			/* A JAR path */
			String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR
																							// file
			JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
			Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.startsWith(path)) { // filter according to the path
					String entry = name.substring(path.length());
					int checkSubdir = entry.indexOf("/");
					if (checkSubdir >= 0) {
						// if it is a subdirectory, we just return the directory name
						entry = entry.substring(0, checkSubdir);
					}
					result.add(entry);
				}
			}
			return result.toArray(new String[result.size()]);
		}
		throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
	}

}
