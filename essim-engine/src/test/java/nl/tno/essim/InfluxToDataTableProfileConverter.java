package nl.tno.essim;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.utils.ESDLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class InfluxToDataTableProfileConverter {

    public static void main(String[] args) throws IOException {
        String esdl = Files.readString(Path.of("/home/armin/dev/essim/Eindhoven simple - meerdere carriers wb.esdl"));

        convertInfluxToPostgres((EnergySystem) ESDLUtil.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8))).getContents().getFirst());
        convertInfluxToInflux((EnergySystem) ESDLUtil.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8))).getContents().getFirst());
    }

    @NotNull
    static String convertInfluxToPostgres(EnergySystem energySystem) throws IOException {
        HashMap<String, String> postgresMap = new HashMap<>();
        postgresMap.put("http://geis:8086", "postgresql://localhost:5432/essim");
        postgresMap.put("http://localhost:8086", "postgresql://localhost:5432/essim");
        postgresMap.put("http://influxdb:8086", "postgresql://localhost:5432/essim");
        convert(energySystem, postgresMap, DatabaseTypeEnum.POSTGRESQL);
        String newEsdl = ESDLUtil.ecoreResourceToString(energySystem);
        Files.writeString(Path.of("/home/armin/dev/essim/Eindhoven simple - meerdere carriers wb_converted_to_postgres.esdl"), newEsdl);
        return newEsdl;
    }

    @NotNull
    static String convertInfluxToInflux(EnergySystem energySystem) throws IOException {
        HashMap<String, String> postgresMap = new HashMap<>();
        postgresMap.put("http://geis:8086", "http://localhost:8086");
        postgresMap.put("http://localhost:8086", "http://localhost:8086");
        postgresMap.put("http://influxdb:8086", "http://localhost:8086");
        convert(energySystem, postgresMap, DatabaseTypeEnum.INFLUXDB);
        String newEsdl = ESDLUtil.ecoreResourceToString(energySystem);
        Files.writeString(Path.of("/home/armin/dev/essim/Eindhoven simple - meerdere carriers wb_converted_to_influx.esdl"), newEsdl);
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
                        dataTableProfile.setTimeColumnName("time");
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
