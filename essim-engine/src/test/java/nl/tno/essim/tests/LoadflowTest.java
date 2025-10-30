package nl.tno.essim.tests;

import esdl.Asset;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.Port;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class LoadflowTest {

    static HashMap<String, List<String>> expectedCurves;
    private static AtomicInteger counter = new AtomicInteger(1);


    static EnergySystem runTest(EssimSimulation sim, InMemoryObservationConsumer inMemoryObservationConsumer) throws Exception {
        IMongoBackend mongo = new MongoBackendStub();
        sim.setPowsyblLoadFlow(true);
        if (System.getenv("INFLUX_URL") != null) {
            sim.setInfluxURL(System.getenv("INFLUX_URL"));
        }
        if (System.getenv("POSTGRES_URL") != null) {
            sim.setPostgresURL(System.getenv("POSTGRES_URL"));
        }

        String simId = mongo.addSimulation(sim);
        ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, sim.getEsdlContents(), mongo);
        essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);

        essimEngine.startSimulation();
        while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
            Thread.sleep(100);
        }

        String grafanaDashboard = essimEngine.createGrafanaDashboard();
        System.out.println("Grafana  dashboard at: " + grafanaDashboard);

        EnergySystem es = essimEngine.getEnergySystem();
        EList<Asset> assets = es.getInstance().get(0).getArea().getAsset();
        assets.stream().filter(a -> a instanceof EnergyAsset).map(EnergyAsset.class::cast).forEach(a -> {
            for (Port p : a.getPort()) {
                System.out.println(a.getName() + " " + p.getProfile());
            }
        });

        return es;
    }

    //	@Test
    void testLoadFlow1() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/Loadflow-1.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "1b");

        EnergySystem energySystem = runTest(sim, inMemoryObservationConsumer);
        // TODO: Add checks
    }

    //	@Test
    void testLoadFlowEindhovenMSOnly() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/LoadflowEindhovenMSOnly_timeseries.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 2, 0, 0, 0)));
        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "2a_" + Integer.toHexString(UUID.randomUUID().hashCode()));

        EnergySystem energySystem = runTest(sim, inMemoryObservationConsumer);
    }

    //	@Test
    void testLoadFlowWithConversion() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/Loadflow-with-conversion.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "3a");

        EnergySystem energySystem = runTest(sim, inMemoryObservationConsumer);

//		String resultESDL = ESSimEngine.ecoreResourceToString(energySystem);
//		System.out.println("Resulting ESDL: \n" + resultESDL);
    }

    //	@Test
    void testLoadFlowWithTransformer() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/Loadflow2-timeseries.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 2, 0, 0, 0)));
        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "-WT");

        EnergySystem energySystem = runTest(sim, inMemoryObservationConsumer);
        // TODO: add checks

    }

    //	@Test
    void testLoadFlowWithTransformer2() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/Loadflow3.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2015, 1, 2, 0, 0, 0)));
        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "-WT2");

        EnergySystem energySystem = runTest(sim, inMemoryObservationConsumer);

//		String resultESDL = ESSimEngine.ecoreResourceToString(energySystem);
//		System.out.println("Resulting ESDL: \n" + resultESDL);
    }

    @Test
    void testLoadFlowSkippingSteps() throws Exception {
        String esdl = new String(LoadflowTest.class.getResourceAsStream("/loadflow4-20230208-1h.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EssimSimulation sim = new EssimSimulation();
        sim.setEsdlContents(esdl);
        ByteArrayInputStream esdlStream = new ByteArrayInputStream(esdl.getBytes(StandardCharsets.UTF_8));
//        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2023, 1, 1, 0, 0, 0)));
//        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2023, 1, 2, 0, 0, 0)));
        XMIResourceImpl esdlResource = ESDLUtil.loadEcoreResource(esdlStream);
        LocalDateTime[] profilesStartAndEnd = ESDLUtil.getProfilesStartAndEnd(esdlResource);
        log.info("Profiles start and end: " + profilesStartAndEnd[0] + " - " + profilesStartAndEnd[1]);
        sim.setStartDate(EssimTime.localDateTimeToDate(profilesStartAndEnd[0]));
        sim.setEndDate(EssimTime.localDateTimeToDate(profilesStartAndEnd[1]));

        sim.setScenarioID(LoadflowTest.class.getSimpleName() + "-WT3");

        IMongoBackend mongo = new MongoBackendStub();
        sim.setPowsyblLoadFlow(true);
        if (System.getenv("INFLUX_URL") != null) {
            sim.setInfluxURL(System.getenv("INFLUX_URL"));
        }
        if (System.getenv("POSTGRES_URL") != null) {
            sim.setPostgresURL(System.getenv("POSTGRES_URL"));
        }

        String simId = mongo.addSimulation(sim);
        ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, sim.getEsdlContents(), mongo);
        EnergySystem es = essimEngine.getEnergySystem();

        essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);

        essimEngine.startSimulation();
        while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
            Thread.sleep(100);
        }

        String grafanaDashboard = essimEngine.createGrafanaDashboard();
        System.out.println("Grafana  dashboard at: " + grafanaDashboard);

        EList<Asset> assets = es.getInstance().get(0).getArea().getAsset();
        assets.stream().filter(a -> a instanceof EnergyAsset).map(EnergyAsset.class::cast).forEach(a -> {
            for (Port p : a.getPort()) {
                System.out.println(a.getName() + " " + p.getProfile());
            }
        });


//		String resultESDL = ESSimEngine.ecoreResourceToString(energySystem);
//		System.out.println("Resulting ESDL: \n" + resultESDL);
    }
}
