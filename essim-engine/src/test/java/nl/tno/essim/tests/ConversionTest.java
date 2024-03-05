package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import esdl.Asset;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.Port;
import esdl.impl.AbstractBasicConversionImpl;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulationImpl;
import nl.tno.essim.model.MarketImpl;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

class ConversionTest {

	static HashMap<String, List<String>> expectedCurves;
	private static AtomicInteger counter = new AtomicInteger(1);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		expectedCurves = new HashMap<String, List<String>>();
		expectedCurves.put("Levering", Arrays.asList(new String[] { "", }));
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	static EnergySystem runTest(File esdlFile, MarketImpl market,
			InMemoryObservationConsumer inMemoryObservationConsumer) throws Exception {
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulationImpl sim = new EssimSimulationImpl();
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
		sim.setScenarioID("TestConversion-" + counter.getAndIncrement());
		sim.setInfluxURL("http://localhost:8086");
		if (market != null) {
			sim.setMarket(market);
		}

		String simId = mongo.addSimulation(sim);
		ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, esdlFile, mongo);
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
		essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);

		essimEngine.startSimulation();
		while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
			Thread.sleep(100);
		}

		// need to sleep as data is still being written in parallel threads and we don't
		// have a handle to that.
		Thread.sleep(1500);

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
				Column<?> column = table.column("bidcurve");
				int i = 0;
//                    for (Object row : column.asList()) {
//                        System.out.println("Bidcurve " + i + " " + asset.getName() + ": " + row);
//                        list.add(row.toString());
//                        i++;
//                    }
			}
			if ("GenericConversion_7e87".equals(asset.getName())) {
				assertEquals((-42 + 70) * 3600000, total, 1E3);
			} else if ("Import_5587".equals(asset.getName())) {
				assertEquals(-70 * 3600000, total, 1E3);
			} else if ("EElectricityDemand_7321".equals(asset.getName())) {
				assertEquals(46 * 3600000, total, 1E3);
			}
		}
//            assertEquals(expectedCurves, bidcurveMap);
	}

	private void checkResultsDBS(InMemoryObservationConsumer inMemoryObservationConsumer, EnergySystem es) {
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
//			if (asset instanceof AbstractBasicConversionImpl) {
			List<String> list = bidcurveMap.get(asset.getName());
			if (list == null) {
				list = new ArrayList<String>();
				bidcurveMap.put(asset.getName(), list);
			}
			Column<?> column = table.column("bidcurve");
			int i = 0;
			for (Object row : column.asList()) {
				System.out.println("Bidcurve " + i + " " + asset.getName() + ": " + row);
				list.add(row.toString());
				i++;
			}
//			}
			if ("GenericConversion_7e87".equals(asset.getName())) {
				assertEquals(9.4 * 3600000, total, 1E3);
			} else if ("Import_e249".equals(asset.getName())) {
				assertEquals(-(175 - 47 * .8 + 6) * 3600000, total, 1E3);
			} else if ("ElectricityDemand_7321".equals(asset.getName())) {
				assertEquals(175 * 3600000, total, 1E3);
			} else if ("GasProducer_87be".equals(asset.getName())) {
				assertEquals(-49.5 * 3600000, total, 1E3);
			}
		}
//            assertEquals(expectedCurves, bidcurveMap);
	}

	private void checkImbalance(InMemoryObservationConsumer inMemoryObservationConsumer,
			double expectedElectricityImbalance) {
		HashMap<String, Table> networks = inMemoryObservationConsumer.getNetworks();
		double electricitySum = networks.get("Test conversion Electricity Network 0").doubleColumn("imbalanceEnergy")
				.sum();
		assertEquals(expectedElectricityImbalance, electricitySum, 1e3);
		double gasSum = networks.get("Test conversion Gas Network 0").doubleColumn("imbalanceEnergy").sum();
		assertEquals(0, gasSum, 1);
	}

	@Test
	void testConversion() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversion.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, null, inMemoryObservationConsumer);
		checkResultsDBD(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
	}

//	@Test
	void testConversionMarketPrices() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversion.esdl");
		MarketImpl market = new MarketImpl();
		market.setMinPrice(0.0);
		market.setMaxPrice(1E-6);
		market.setDeltaPrice(1E-12);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, market, inMemoryObservationConsumer);
		checkResultsDBD(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
	}

	@Test
	void testConversion2() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversion2.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, null, inMemoryObservationConsumer);
		checkResultsDBD(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, 4 * 3600000);
	}

	@Test
	void testConversion3() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversion3.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, null, inMemoryObservationConsumer);
		checkResultsDBD(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, 0);

	}

	@Test
	void testConversionDBS() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversionDBS.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, null, inMemoryObservationConsumer);
		checkResultsDBS(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, 0);

	}

	@Test
	void testConversionDBSImbalance() throws Exception {
		File esdlFile = new File("src/test/resources/TestConversionDBS2.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdlFile, null, inMemoryObservationConsumer);
		checkResultsDBS(inMemoryObservationConsumer, energySystem);
		checkImbalance(inMemoryObservationConsumer, -6*3600000);
	}

}
