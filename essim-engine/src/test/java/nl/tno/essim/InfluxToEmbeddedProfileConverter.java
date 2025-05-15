package nl.tno.essim;

import esdl.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
public class InfluxToEmbeddedProfileConverter {

    public static void main(String[] args) throws IOException {
        if (args.length <= 0 || Files.notExists(Path.of(args[0]))) {
            System.err.println("Usage: " + InfluxToEmbeddedProfileConverter.class.getSimpleName() + " <file>");
            System.exit(1);
        }
        Path path = Path.of(args[0]);


        String esdl = Files.readString(path);

        XMIResourceImpl xmiResource = ESDLUtil.loadEcoreResource(new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8)));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXX");
        LocalDateTime start = OffsetDateTime.parse("2015-01-01T00:00:00.000000+0100", formatter).toLocalDateTime();
        LocalDateTime end = OffsetDateTime.parse("2015-01-02T00:00:00.000000+0100", formatter).toLocalDateTime();
        EssimTime essimTime = new EssimTime(start, end, EssimDuration.of(15, ChronoUnit.MINUTES));
        ESDLUtil.convertExternalToTimeSeriesProfiles(xmiResource, essimTime);
        String newEsdl = ESDLUtil.ecoreResourceToString((EnergySystem) xmiResource.getContents().getFirst());
        String outName = path.toString().replaceAll("\\.esdl$", "_timeseries.esdl");
        log.info("Writing new ESDL to: {}", outName);
        Files.writeString(Path.of(outName), newEsdl);
    }

}
