package nl.tno.essim.tests;

import esdl.*;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicPricesTest2 {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	
	
	
	@Test
	void testActualUseCase() throws Exception {
						
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulation sim = new EssimSimulation();
		if (System.getenv("INFLUX_URL") != null) {
			sim.setInfluxURL(System.getenv("INFLUX_URL"));
		}
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 2 ,19, 0,0,0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(  LocalDateTime.of(2019, 2, 20, 0,0,0)));
		Market market = new Market();
		market.setMinPrice(-110.0);
		market.setMaxPrice(100.0);
		market.setDeltaPrice(1E-10);
		sim.setMarket(market);
		
		sim.setScenarioID("testcase3-realpriceprofile");
		sim.setSimulationDescription("testcase3: Real Priceprofile Dp=1E-10");
		String simId = mongo.addSimulation(sim);
		String esdl = new String(DynamicPricesTest2.class.getResourceAsStream("/testcase1/GH-HP-HD-CarrierCosts-TimeSeriesProfile.esdl").readAllBytes(), StandardCharsets.UTF_8);
		ESSimEngine essimEngine = new ESSimEngine("local-realprices-testcase1", simId, sim, esdl, mongo);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
		essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);

		String grafanaDashboard = "";
		if (System.getenv("INFLUX_URL") != null) {
            grafanaDashboard = essimEngine.createGrafanaDashboard();
		}

		essimEngine.startSimulation();
		while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
			Thread.sleep(100);
		}
		
		Thread.sleep(1000);
		EnergySystem es = essimEngine.getEnergySystem();
		EList<Asset> assets = es.getInstance().get(0).getArea().getAsset();
		assets.stream().filter(a -> a instanceof EnergyAsset).map(EnergyAsset.class::cast).forEach(a -> {
			for (Port p: a.getPort()) {
					System.out.println(a.getName() + " " + p.getProfile());
			}
		});
		
		HashMap<String,Table> tables = inMemoryObservationConsumer.getAssets();
		System.out.println("Assets: " + tables.keySet());
		System.out.println("Totals:");
		LocalDateTime start = tables.values().stream().findAny().get().dateTimeColumn(0).get(0);
		LocalDateTime stop = tables.values().stream().findAny().get().dateTimeColumn(0).last(1).get(0);
		long simHours = ChronoUnit.HOURS.between(start, stop);
		assertEquals(24, simHours);
		System.out.println("Sim hours:" + simHours);
		System.out.println("\t(" + start + " to " + stop + ")");
		
		CsvWriter csvWriter = new CsvWriter();
		Files.createDirectories(Path.of("output"));

		// validate output of simulation by iterating through the tables
		for (Entry<String, Table> entry : tables.entrySet()) {			
			Table table = entry.getValue();
			DoubleColumn allocationEnergy = table.doubleColumn("allocationEnergy");
			System.out.println("\t" + entry.getKey());
			System.out.println(Arrays.toString(allocationEnergy.asDoubleArray()));
			double total = allocationEnergy.sum();
			System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
			Resource eResource = es.eResource();
			EnergyAsset asset = (EnergyAsset)eResource.getEObject(entry.getKey());
			if (asset instanceof HeatingDemand) {
				assertEquals(total, 250E6, 1E3);
			} else if (asset instanceof Import && asset.getName().equals("Import_263a")) { // gas import
				assertEquals(-147E6, total, 1E6); // 1 MJ difference
			} else if (asset instanceof Import && asset.getName().equals("Import_cb54")) { // E import
				assertEquals(-55E6, total, 1E6);
			} else if (asset instanceof HeatPump) {
				var carriers = table.stringColumn("carrierName");				
				var total_elek = allocationEnergy.where(carriers.equalsIgnoreCase("Elek")).sum();
				var total_heat = allocationEnergy.where(carriers.equalsIgnoreCase("Heat")).sum();
				System.out.println("Total HP Elek:" + total_elek);
				System.out.println("Total HP Heat:" + total_heat);
				assertEquals(55E6, total_elek, 1E6);
				assertEquals(-110E6, total_heat, 1E6);
			} else if (asset instanceof GasHeater) {
				var carriers = table.stringColumn("carrierName");				
				var total_gas = allocationEnergy.where(carriers.equalsIgnoreCase("Gas")).sum();
				var total_heat = allocationEnergy.where(carriers.equalsIgnoreCase("Heat")).sum();
				System.out.println("Total GH gas:" + total_gas);
				System.out.println("Total GH Heat:" + total_heat);
				assertEquals(147E6, total_gas, 1E6);
				assertEquals(-140E6, total_heat, 1E6);
			}
			csvWriter.write(table, CsvWriteOptions.builder("output/" + asset.getName() + ".csv").separator(';').build());
		}

		HashMap<String,Table> networks = inMemoryObservationConsumer.getNetworks();
		for (Entry<String, Table> network : networks.entrySet()) {
			Table table = network.getValue();
			double sum = table.doubleColumn("imbalanceEnergy").sum();
			System.out.println("Network " + network.getKey() + " balance = " + sum);
			assertEquals(0.0, sum, 1.0); // max 1 joule difference
			csvWriter.write(table, CsvWriteOptions.builder("output/" + network.getKey() + ".csv").separator(';').build());
		}
		if (System.getenv("INFLUX_URL") != null) {
			System.out.println("Grafana  dashboard at: " + grafanaDashboard);
		}

	}
}
