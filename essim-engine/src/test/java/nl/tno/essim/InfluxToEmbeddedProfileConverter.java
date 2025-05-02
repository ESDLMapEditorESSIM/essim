package nl.tno.essim;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.profiles.ProfileDataHandler;
import nl.tno.essim.profiles.ProfileDataManager;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.util.Converter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static nl.tno.essim.InfluxToDataTableProfileConverter.getDataConfigurations;

@Slf4j
public class InfluxToEmbeddedProfileConverter {

    public static void main(String[] args) throws IOException {
        if (args.length <= 0 || Files.notExists(Path.of(args[0]))) {
            System.err.println("Usage: " + InfluxToEmbeddedProfileConverter.class.getSimpleName() + " <file>");
            System.exit(1);
        }
        Path path = Path.of(args[0]);


        String esdl = Files.readString(path);

        XMIResourceImpl xmiResource = ESSimEngine.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8)));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXX");
        LocalDateTime start = OffsetDateTime.parse("2019-01-01T00:00:00.000000+0100", formatter).toLocalDateTime();
        LocalDateTime end = OffsetDateTime.parse("2019-01-02T00:00:00.000000+0100", formatter).toLocalDateTime();
        EssimTime essimTime = new EssimTime(start, end, EssimDuration.of(1, ChronoUnit.HOURS));
        convert(xmiResource, essimTime);
        String newEsdl = ESSimEngine.ecoreResourceToString((EnergySystem) xmiResource.getContents().getFirst());
        String outName = path.toString().replaceAll("\\.esdl$", "_timeseries.esdl");
        log.info("Writing new ESDL to: {}", outName);
        Files.writeString(Path.of(outName), newEsdl);
    }

    static void convert(XMIResourceImpl xmiResource, EssimTime essimTime) {
        EnergySystem energySystem = (EnergySystem) xmiResource.getContents().getFirst();
        Instance instance = energySystem.getInstance().getFirst();
        if (instance.getArea() == null) {
            throw new IllegalArgumentException("No area found in ESDL energy instance !");
        }
        Area mainArea = instance.getArea();
        HashMap<String, DatabaseConfiguration> map = new HashMap<>();
        ProfileDataManager profileDataManager = new ProfileDataManager(xmiResource, essimTime);
        ESSimEngine.visitAllEnergyAssets(mainArea, energyAsset -> {
            for (Port port : energyAsset.getPort()) {
                ArrayList<GenericProfile> newProfiles = new ArrayList<>();
                for (var i = port.getProfile().iterator(); i.hasNext(); ) {
                    GenericProfile origProfile = i.next();
                    if (origProfile instanceof InfluxDBProfile || origProfile instanceof DataTableProfile) {
                        ProfileDataHandler profileDataSupplier = profileDataManager.getProfileDataSupplier(origProfile);
                        EList<ProfileElement> profileElements = profileDataSupplier.getProfileElements(EssimTime.localDateTimeToDate(essimTime.getSimulationStartTime()), EssimTime.localDateTimeToDate(essimTime.getSimulationEndTime()), Converter.toESDLDuration(essimTime.getSimulationStepLength()));
                        TimeSeriesProfile profile = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
                        profile.setTimestep((int) essimTime.getSimulationStepLength().getSeconds());
                        profile.setStartDateTime(EssimTime.localDateTimeToDate(essimTime.getSimulationStartTime()));
                        profile.setProfileType(origProfile.getProfileType());
                        profile.setProfileQuantityAndUnit(profile.getProfileQuantityAndUnit());
                        profile.setInterpolationMethod(profile.getInterpolationMethod());
                        profileDataManager.getProfileDataSupplier(profile).setProfileElements(profileElements);
                        i.remove();
                        newProfiles.add(profile);
                    }
                }
                port.getProfile().addAll(newProfiles);
            }
        });
    }
}
