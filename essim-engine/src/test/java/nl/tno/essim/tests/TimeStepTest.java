package nl.tno.essim.tests;

import esdl.Asset;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.Port;
import esdl.impl.AbstractBasicConversionImpl;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeStepTest {

    private static AtomicInteger counter = new AtomicInteger(1);


    static EnergySystem runTest(String esdl, Market market,
                                InMemoryObservationConsumer inMemoryObservationConsumer, String timeStep) throws Exception {
        IMongoBackend mongo = new MongoBackendStub();
        EssimSimulation sim = new EssimSimulation();
        sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
        sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
        sim.setTimeStep("15m");
        sim.setScenarioID("TestConversionTimeStep-" + timeStep + "-" + counter.getAndIncrement());
        if (System.getenv("INFLUX_URL") != null) {
            sim.setInfluxURL(System.getenv("INFLUX_URL"));
        }
        if (market != null) {
            sim.setMarket(market);
        }

        String simId = mongo.addSimulation(sim);
        ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, esdl, mongo);
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

    @Test
    void testConversion15m() throws Exception {
        String esdl = new String(TimeStepTest.class.getResourceAsStream("/TestConversion.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EnergySystem energySystem = runTest(esdl, null, inMemoryObservationConsumer, "15m");
        checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
        checkResultsDBD(inMemoryObservationConsumer, energySystem);

        String resultESDL = ESDLUtil.ecoreResourceToString(energySystem);
        System.out.println("Resulting ESDL: \n" + resultESDL);
    }


    @Test
    void testConversion35m() throws Exception {
        String esdl = new String(TimeStepTest.class.getResourceAsStream("/TestConversion.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EnergySystem energySystem = runTest(esdl, null, inMemoryObservationConsumer, "25m");
        checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
        checkResultsDBD(inMemoryObservationConsumer, energySystem);

        String resultESDL = ESDLUtil.ecoreResourceToString(energySystem);
        System.out.println("Resulting ESDL: \n" + resultESDL);
    }


    @Test
    void testConversion2h() throws Exception {
        String esdl = new String(TimeStepTest.class.getResourceAsStream("/TestConversion.esdl").readAllBytes(), StandardCharsets.UTF_8);
        InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
        EnergySystem energySystem = runTest(esdl, null, inMemoryObservationConsumer, "2h");
        checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
        checkResultsDBD(inMemoryObservationConsumer, energySystem);

        String resultESDL = ESDLUtil.ecoreResourceToString(energySystem);
        System.out.println("Resulting ESDL: \n" + resultESDL);
    }


    private void checkResultsDBD(InMemoryObservationConsumer inMemoryObservationConsumer, EnergySystem es) {
        HashMap<String, Table> tables = inMemoryObservationConsumer.getAssets();
        System.out.println("Assets: " + tables.keySet());
        LocalDateTime start = tables.values().stream().findAny().get().dateTimeColumn(0).get(0);
        LocalDateTime stop = tables.values().stream().findAny().get().dateTimeColumn(0).last(1).get(0);
        long simHours = ChronoUnit.HOURS.between(start, stop);
        System.out.println("Totals:");
        System.out.println("Sim hours:" + simHours);
        assertEquals(24, simHours);
        System.out.println("\t(" + start + " to " + stop + ")");

        HashMap<String, List<String>> bidcurveMap = new HashMap<>();

        for (Entry<String, Table> entry : tables.entrySet()) {
            Table table = entry.getValue();
            double total = table.doubleColumn("allocationEnergy").sum();
            System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
            Resource eResource = es.eResource();
            EnergyAsset asset = (EnergyAsset) eResource.getEObject(entry.getKey());
            if (asset instanceof AbstractBasicConversionImpl) {
                List<String> list = bidcurveMap.get(asset);
                if (list == null) {
                    list = new ArrayList<String>();
                    bidcurveMap.put(asset.getName(), list);
                }
            }
            if ("GenericConversion_7e87".equals(asset.getName())) {
                assertEquals((-42 + 70) * 3600000, total, 1E3);
            } else if ("Import_5587".equals(asset.getName())) {
                assertEquals(-70 * 3600000, total, 1E3);
            } else if ("EElectricityDemand_7321".equals(asset.getName())) {
                assertEquals(46 * 3600000, total, 1E3);
            }
        }
    }

    private void checkImbalance(InMemoryObservationConsumer inMemoryObservationConsumer,
                                double expectedElectricityImbalance) {
        HashMap<String, Table> networks = inMemoryObservationConsumer.getNetworks();
        double electricitySum = networks.get("Test conversion Electricity Network 1").doubleColumn("imbalanceEnergy")
                .sum();
        assertEquals(expectedElectricityImbalance, electricitySum, 1e3);
        double gasSum = networks.get("Test conversion Gas Network 0").doubleColumn("imbalanceEnergy").sum();
        assertEquals(0, gasSum, 1);
    }


}
