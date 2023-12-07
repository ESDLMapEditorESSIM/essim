package nl.tno.essim.tests;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import esdl.Asset;
import esdl.ElectricityDemand;
import esdl.ElectricityNetwork;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.Export;
import esdl.Import;
import esdl.Port;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulationImpl;
import nl.tno.essim.model.MarketImpl;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;


class RealPricesTest {
	
	
	@Test
	void testReadProfiles( ) throws InterruptedException {
		CsvReadOptions csvReadOptions = CsvReadOptions.builder("src/test/resources/profiles/consumer_dynamic_tariff.csv").separator(';').build();
		Table tariffs = Table.read().csv(csvReadOptions);
		System.out.println(tariffs.print());
	}
	
	

	
	@Test
	void testHighPrices() throws Exception {
		// read price profiles
			
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulationImpl sim = new EssimSimulationImpl();
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1 ,1, 0,0,0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2020, 1 ,1, 0,0,0)));
		MarketImpl market = new MarketImpl();
		sim.setMarket(market);
		market.setMinPrice(-100.0);
		market.setMaxPrice(100.0);
		market.setDeltaPrice(1E-12);
		
		sim.setScenarioID("testcase1-realpriceprofile");
		String simId = mongo.addSimulation(sim);
		File esdlFile = new File("src/test/resources/ED-Imp-Exp-highprices.esdl");
		ESSimEngine essimEngine = new ESSimEngine("local-realprices-test", simId, sim, esdlFile, mongo);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
		essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);
		
		essimEngine.startSimulation();
		while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
			Thread.sleep(100);
		}
		
		Thread.sleep(1000);
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
		assertEquals(8760, simHours);
		System.out.println("Sim hours:" + simHours);
		System.out.println("\t(" + start + " to " + stop + ")");
		
		Files.createDirectories(Paths.get("output"));
		for (Entry<String, Table> entry : tables.entrySet()) {			
			Table table = entry.getValue();
			double total = table.doubleColumn("allocationEnergy").sum();
			System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
			Resource eResource = es.eResource();
			EnergyAsset asset = (EnergyAsset)eResource.getEObject(entry.getKey());
			if (asset instanceof Export) {
				assertEquals(total, 0.0, 1E-3);
			} else if (asset instanceof ElectricityDemand || asset instanceof ElectricityNetwork) {
				assertEquals(total, 1E9, 1E3);
			} else if (asset instanceof Import) {
				assertEquals(-1E9, total, 1E9);
			}
			table.write().csv("output/" + table.name() + ".csv");
		}
		
		HashMap<String,Table> networks = inMemoryObservationConsumer.getNetworks();
		for (Entry<String, Table> network : networks.entrySet()) {
			Table table = network.getValue();
			double sum = table.doubleColumn("imbalanceEnergy").sum();
			System.out.println("Network " + network.getKey() + " balance = " + sum);
			assertEquals(0.0, sum, 1.0);
			table.write().csv("output/" + table.name() + ".csv");
		}
		/**
		 * 30.000000000001=-5.4E7
		 * 30.000000000000004
		 *  Export_66b0: [curve={-100.0=5.4E7, 0.0=5.4E7, 20.0=0.0, 100.0=0.0}; mc=20.0], price=30.000000000000004, energy=0.0 (Node.java:340)
11:35:56.622 [pool-1-thread-4] TRACE n.t.essim.transportsolver.nodes.Node - 2019-12-31T21:00 - Import_7e47: [curve={-100.0=0.0, 30.0=0.0, 30.000000000001=-5.4E7, 100.0=-5.4E7}; mc=30.0], price=30.000000000000004, energy=-192170.81850533807 (Node.java:340)
11:35:56.622 [pool-1-thread-4] TRACE n.t.essim.transportsolver.nodes.Node - 2019-12-31T21:00 - ElectricityNetwork_973e: [curve={-100.0=5.417973E7, 0.0=5.417973E7, 20.0=179730.0, 100.0=179730.0}; mc=0.0], price=30.000000000000004, energy=179730.0 (Node.java:340)
11:35:56.622 [pool-1-thread-4] TRACE n.t.essim.transportsolver.nodes.Node - 2019-12-31T21:00 - ElectricityDemand_c106: [curve={-100.0=179730.0, 100.0=179730.0}; mc=0.0], price=30.000000000000004, energy=179730.0 (Node.java:340)
		 * 
		 */
		
	}

}
