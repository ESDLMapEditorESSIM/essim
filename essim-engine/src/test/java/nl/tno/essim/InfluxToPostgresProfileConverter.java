package nl.tno.essim;

import esdl.*;
import lombok.extern.slf4j.Slf4j;

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
public class InfluxToPostgresProfileConverter {

    public static void main(String[] args) throws IOException {
        String esdl = Files.readString(Path.of("/home/armin/dev/essim/Tutorial5_Scenario - elektrolyzer issue.esdl"));
        EnergySystem energySystem = ESSimEngine.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8)));
        InfluxToPostgresProfileConverter converter = new InfluxToPostgresProfileConverter();
        HashMap<String, String> urlMap = new HashMap<>();
//        urlMap.put("http://localhost:8086", "postgresql://localhost:15432/essim");
//        urlMap.put("http://influxdb:8086", "postgresql://localhost:15432/essim");
        converter.convert(energySystem, urlMap);
        String newEsdl = ESSimEngine.ecoreResourceToString(energySystem);
        Files.writeString(Path.of("/home/armin/dev/essim/Tutorial5_Scenario - elektrolyzer issue_converted_nochange.esdl"), newEsdl);
        log.info("New ESDL:\n{}", newEsdl);
    }

    private void convert(EnergySystem energySystem, HashMap<String, String> urlMap) {
        Instance instance = energySystem.getInstance().getFirst();
        if (instance.getArea() == null) {
            throw new IllegalArgumentException("No area found in ESDL energy instance !");
        }
        Area mainArea = instance.getArea();
        HashMap<String, DatabaseConfiguration> map = new HashMap<>();
        ESSimEngine.visitAllEnergyAssets(mainArea, energyAsset -> {
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
                            dbc = EsdlFactory.eINSTANCE.createDatabaseConfiguration();
                            dbc.setType(DatabaseTypeEnum.POSTGRESQL);
                            dbc.setId(UUID.randomUUID().toString());
                            URI uri = URI.create(newUri);
                            dbc.setHost(uri.getHost());
                            if (uri.getPort() > 0) {
                                dbc.setPort(uri.getPort());
                            }
                            String dbName = uri.getPath().replaceFirst("/", "");

                            if (!dbName.isEmpty()) {
                                dbc.setDatabase(dbName);
                            }
                            String query = uri.getQuery();
                            if (query != null && query.contains("tls")) {
                                // Parse the query string to find the value of 'tls'
                                String[] queryParams = query.split("&");
                                for (String param : queryParams) {
                                    String[] keyValue = param.split("=");
                                    if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("tls")) {
                                        // Set the tls value on dbc based on the URI's tls value
                                        dbc.setTls(Boolean.parseBoolean(keyValue[1]));
                                        break;
                                    }
                                }
                            }
                            getDataConfigurations(energySystem).getConfigurations().add(dbc);
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
                        newProfiles.add(dataTableProfile);
                    }
                }
                port.getProfile().addAll(newProfiles);
            });
        });
    }

    private DataConfigurations getDataConfigurations(EnergySystem energySystem) {
        DataConfigurations dataConfigurations = energySystem.getEnergySystemInformation().getDataconfigurations();
        if (dataConfigurations == null) {
            dataConfigurations = EsdlFactory.eINSTANCE.createDataConfigurations();
            energySystem.getEnergySystemInformation().setDataconfigurations(dataConfigurations);
        }
        return dataConfigurations;
    }
}
