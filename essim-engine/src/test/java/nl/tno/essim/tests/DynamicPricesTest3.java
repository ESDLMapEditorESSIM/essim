package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import esdl.Asset;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.HeatingDemand;
import esdl.Import;
import esdl.Port;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulationImpl;
import nl.tno.essim.model.MarketImpl;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

class DynamicPricesTest3 {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	
	
	
	@Test
	void testActualUseCaseWithInfluxAndGrafana() throws Exception {
						
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulationImpl sim = new EssimSimulationImpl();
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 2 ,19, 0,0,0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(  LocalDateTime.of(2019, 2, 20, 0,0,0)));
		MarketImpl market = new MarketImpl();
		market.setMinPrice(-100.0);
		market.setMaxPrice(100.0);
		market.setDeltaPrice(1E-10);
		sim.setMarket(market);
		
		sim.setScenarioID("testcase3-realpriceprofile");
		sim.setSimulationDescription("testcase3: Real Priceprofile Dp=1E-10");
		String simId = mongo.addSimulation(sim);
		File esdlFile = new File("src/test/resources/testcase1/GH-HP-HD-CarrierCosts-TimeSeriesProfile.esdl");
		sim.setInfluxURL("http://localhost:8086");
		
		ESSimEngine essimEngine = new ESSimEngine("local-realprices-testcase3", simId, sim, esdlFile, mongo);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
		essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);
		
		essimEngine.startSimulation();
		while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
			Thread.sleep(100);
		}
						
		Thread.sleep(1000);
		
		String grafanaDashboard = essimEngine.createGrafanaDashboard();
		System.out.println("Grafana  dashboard at: " + grafanaDashboard);
		
		
		EnergySystem es = essimEngine.getEnergySystem();
		EList<Asset> assets = es.getInstance().get(0).getArea().getAsset();
		assets.stream().filter(a -> a instanceof EnergyAsset).map(EnergyAsset.class::cast).forEach(a -> {
			//if (a.getName().equals("ElectricityDemand_c106")) {
			for (Port p: a.getPort()) {
				//a.getPort().stream().filter(InPort.class::isInstance).map(InPort.class::cast).findFirst().ifPresent(inPort -> {
					System.out.println(a.getName() + " " + p.getProfile());
				//});
			//}
			}
		});
		
		// Export: 7e47b79a-4cde-4db9-81ae-67fd40a92de0
		// The network does not have allocationPower,
		//String export_id = "7e47b79a-4cde-4db9-81ae-67fd40a92de0";
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

		for (Entry<String, Table> entry : tables.entrySet()) {			
			Table table = entry.getValue();
			double total = table.doubleColumn("allocationEnergy").sum();
			System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
			Resource eResource = es.eResource();
			EnergyAsset asset = (EnergyAsset)eResource.getEObject(entry.getKey());
			if (asset instanceof HeatingDemand) {
				assertEquals(total, 250E6, 1E3);
			} else if (asset instanceof Import && asset.getName().equals("Import_263a")) { // gas import
				//assertEquals(-131.6E6, total, 1E6);
			} else if (asset instanceof Import && asset.getName().equals("Import_cb54")) { // E import
				//assertEquals(-62.5E6, total, 1E6);
			}
			csvWriter.write(table, CsvWriteOptions.builder("output/" + asset.getName() + ".csv").separator(';').build());
		}
		
		HashMap<String,Table> networks = inMemoryObservationConsumer.getNetworks();
		for (Entry<String, Table> network : networks.entrySet()) {
			Table table = network.getValue();
			double sum = table.doubleColumn("imbalanceEnergy").sum();
			System.out.println("Network " + network.getKey() + " balance = " + sum);
			//assertEquals(0.0, sum, 1.0);
		}
	}
}
