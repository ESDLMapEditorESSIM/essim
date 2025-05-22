/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */

package nl.tno.essim.commons;

import esdl.*;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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

    /**
     * Recursively adds all connected ports to the provided set of ports based on a predicate condition.
     * Ports connected either directly or through the energy assets matching the given predicate
     * are included in the set. The method ensures transitive connections are accounted for until
     * no new ports can be added.
     *
     * @param portSet   the set of ports to which connected ports will be added. This set is updated
     *                  in place, and must not be null.
     * @param predicate a condition used to filter energy assets. Connected ports from energy assets
     *                  that satisfy this predicate are added to the set. Must not be null.
     */
    public static void addConnectedPortsToSet(Set<Port> portSet, Predicate<EnergyAsset> predicate) {
        Set<Port> connected = portSet.stream().flatMap(p -> getConnectedPorts(p).stream()).collect(Collectors.toSet());
        connected.addAll(portSet.stream().filter(p -> predicate.test(p.getEnergyasset())).flatMap(p -> p.getEnergyasset().getPort().stream()).toList());
        int prevSize = portSet.size();
        portSet.addAll(connected);
        if (portSet.size() > prevSize) {
            addConnectedPortsToSet(portSet, predicate);
        }
    }

    /**
     * Retrieves the list of ports that are connected to the specified port. Depending on the type of the input port
     * (either InPort or OutPort), it gathers the connected OutPorts or InPorts, respectively.
     *
     * @param port the port for which the connected ports are to be retrieved. This port should be an instance of either
     *             InPort or OutPort.
     * @return a list containing the ports that are directly connected to the specified port. If the input is of type
     * InPort, the result will include connected OutPorts; if it is of type OutPort, the result will include
     * connected InPorts.
     * @throws IllegalStateException if the specified port is not of type InPort or OutPort.
     */
    public static EList<Port> getConnectedPorts(Port port) {
        EList<Port> ports = new BasicEList<>();
        if (port instanceof InPort) {
            EList<OutPort> connectedTo = ((InPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else if (port instanceof OutPort) {
            EList<InPort> connectedTo = ((OutPort) port).getConnectedTo();
            ports.addAll(connectedTo);
        } else {
            throw new IllegalStateException("Port type " + port.getClass().getSimpleName() + " is not supported");
        }
        return ports;
    }

    public static enum Role {
        TRANSPORT, PRODUCER, CONSUMER, BOTH
    }

    ;

    public static String readFileIntoString(String filename) throws IOException {
        return new String(Objects.requireNonNull(Commons.class.getClassLoader().getResourceAsStream(filename)).readAllBytes());
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
