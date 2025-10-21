package nl.tno.essim;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.utils.ESDLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class InfluxToDataTableProfileConverter {

    /**
     * Generates an output file path by adding a postfix to the filename before the extension.
     *
     * @param inputPath The input file path
     * @param postfix   The postfix to add to the filename
     * @return A new Path with the postfix added before the extension
     */
    public static Path generateOutputPath(Path inputPath, String postfix) {
        String fileName = inputPath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');

        String newFileName;
        if (lastDotIndex > 0) {
            // File has an extension
            String nameWithoutExt = fileName.substring(0, lastDotIndex);
            String extension = fileName.substring(lastDotIndex);
            newFileName = nameWithoutExt + postfix + extension;
        } else {
            // No extension found
            newFileName = fileName + postfix;
        }

        return inputPath.resolveSibling(newFileName);
    }

    public static void main(String[] args) throws IOException {
        Path path = Path.of("/home/armin/dev/essim/new data/Eindhoven simple 2023.esdl");
        String esdl = Files.readString(path);


        convertInfluxToPostgres((EnergySystem) ESDLUtil.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8))).getContents().getFirst(), path);
        convertInfluxToInflux((EnergySystem) ESDLUtil.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8))).getContents().getFirst(), path);
    }

    @NotNull
    static String convertInfluxToPostgres(EnergySystem energySystem, Path origPath) throws IOException {
        HashMap<String, String> postgresMap = new HashMap<>();
        postgresMap.put("http://geis:8086", "postgresql://postgres:5432/essim");
        postgresMap.put("http://localhost:8086", "postgresql://postgres:5432/essim");
        postgresMap.put("http://influxdb:8086", "postgresql://postgres:5432/essim");
        convert(energySystem, postgresMap, DatabaseTypeEnum.POSTGRESQL);
        String newEsdl = ESDLUtil.ecoreResourceToString(energySystem);
        Path convertedToPostgres = generateOutputPath(origPath, "_converted_to_postgres");
        Files.writeString(convertedToPostgres, newEsdl);
        return newEsdl;
    }

    @NotNull
    static String convertInfluxToInflux(EnergySystem energySystem, Path origPath) throws IOException {
        HashMap<String, String> postgresMap = new HashMap<>();
        postgresMap.put("http://geis:8086", "http://influxdb:8086");
        postgresMap.put("http://localhost:8086", "http://indlfuxdb:8086");
        postgresMap.put("http://influxdb:8086", "http://influxdb:8086");
        convert(energySystem, postgresMap, DatabaseTypeEnum.INFLUXDB);
        String newEsdl = ESDLUtil.ecoreResourceToString(energySystem);
        Path convertedToPostgres = generateOutputPath(origPath, "_converted_to_influx");
        Files.writeString(convertedToPostgres, newEsdl);
        return newEsdl;
    }

    static void convert(EnergySystem energySystem, HashMap<String, String> urlMap, DatabaseTypeEnum type) {
        Instance instance = energySystem.getInstance().getFirst();
        if (instance.getArea() == null) {
            throw new IllegalArgumentException("No area found in ESDL energy instance !");
        }
        Area mainArea = instance.getArea();
        HashMap<String, DatabaseConfiguration> map = new HashMap<>();
        ESDLUtil.visitAllEnergyAssets(mainArea, energyAsset -> {
            energyAsset.getPort().forEach(port -> {
                ArrayList<GenericProfile> newProfiles = new ArrayList<>();
                for (var i = port.getProfile().iterator(); i.hasNext(); ) {
                    GenericProfile profile = i.next();
                    if (profile instanceof InfluxDBProfile) {
                        String influxUri = ((InfluxDBProfile) profile).getHost() + ":" + ((InfluxDBProfile) profile).getPort();
                        String newUri = urlMap.get(influxUri);
                        if (newUri == null) {
                            continue;
                        }
                        DatabaseConfiguration dbc = map.get(newUri);
                        if (dbc == null) {
                            dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, newUri, type);
                            map.put(newUri, dbc);

                        }
                        i.remove();
                        DataTableProfile dataTableProfile = EsdlFactory.eINSTANCE.createDataTableProfile();
                        dataTableProfile.setConfiguration(dbc);
                        dataTableProfile.setId(UUID.randomUUID().toString());
                        dataTableProfile.setTableName(((InfluxDBProfile) profile).getMeasurement());
                        dataTableProfile.setColumnName(((InfluxDBProfile) profile).getField());
                        dataTableProfile.setDatetimeColumnName("time");
                        dataTableProfile.setSchema(((InfluxDBProfile) profile).getDatabase());
                        dataTableProfile.setProfileQuantityAndUnit(profile.getProfileQuantityAndUnit());
                        dataTableProfile.setProfileType(profile.getProfileType());
                        dataTableProfile.setStartDate(((InfluxDBProfile) profile).getStartDate());
                        dataTableProfile.setEndDate(((InfluxDBProfile) profile).getEndDate());
                        dataTableProfile.setMultiplier(((InfluxDBProfile) profile).getMultiplier());
                        dataTableProfile.setInterpolationMethod(((InfluxDBProfile) profile).getInterpolationMethod());
                        newProfiles.add(dataTableProfile);
                    }
                }
                port.getProfile().addAll(newProfiles);
            });
        });
    }
}
