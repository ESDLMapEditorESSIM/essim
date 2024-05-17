package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import esdl.Asset;
import esdl.ElectricityDemand;
import esdl.ElectricityNetwork;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.Export;
import esdl.Import;
import esdl.Port;
import esdl.TimeSeriesProfile;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

@Disabled("Test case needs further improvement")
class DynamicPricesTest {


	private TimeSeriesProfile toTimeSeriesProfile(Table table, String columnName) {
		DoubleColumn column = table.doubleColumn(columnName);
		TimeSeriesProfile timeSeriesProfile = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
		timeSeriesProfile.setName(column.name());
		timeSeriesProfile.getValues().addAll(column.asList());
		timeSeriesProfile.setId(UUID.randomUUID().toString());
		LocalDateTime startTime = table.dateTimeColumn(0).get(0);
		LocalDateTime endTime = table.dateTimeColumn(0).get(1);
		timeSeriesProfile.setStartDateTime(EssimTime.localDateTimeToDate(startTime));
		timeSeriesProfile.setTimestep((int)ChronoUnit.SECONDS.between(startTime, endTime));
		
		return timeSeriesProfile;
	}
	
	@Disabled("Test to read profiles")
	@Test
	void readProfiles() {
		CsvReadOptions tariffReadOptions = CsvReadOptions.builder("src/test/resources/profiles/consumer_dynamic_tariff.csv")
				.dateTimeFormat(DateTimeFormatter.ISO_DATE_TIME)
				.separator(';').build();
		Table tariffs = Table.read().csv(tariffReadOptions);
		System.out.println(tariffs.print());
		
		

		DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("d-M-yyyy HH:mm");
		CsvReadOptions profileReadOptions = CsvReadOptions.builder("src/test/resources/profiles/standard_profiles.csv")
				.dateTimeFormat(datetimeFormat)
				.separator(';').build();
		Table profiles = Table.read().csv(profileReadOptions);
		//DateTimeColumn dt = profiles.stringColumn(0).map(e -> LocalDateTime.parse(e, datetimeFormat), DateTimeColumn::create);
		//profiles.removeColumns(0);
		//profiles.insertColumn(0, dt);
		System.out.println(profiles.print());
		
	}
	
	
	@Test
	void testActualUseCase() throws Exception {
		CsvReadOptions tariffReadOptions = CsvReadOptions.builder("src/test/resources/profiles/consumer_dynamic_tariff.csv")
				.dateTimeFormat(DateTimeFormatter.ISO_DATE_TIME)
				.separator(';').build();
		Table tariffs = Table.read().csv(tariffReadOptions);
		tariffs.print();
		
//		DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("d-M-yyyy HH:mm");
//		CsvReadOptions profileReadOptions = CsvReadOptions.builder("src/test/resources/profiles/standard_profiles.csv")
//				.dateTimeFormat(datetimeFormat)
//				.separator(';').build();
//		Table profiles = Table.read().csv(profileReadOptions);
		//profiles.print();
		
		
		
		//DoubleColumn e_price_EUR_kWh = tariffs.doubleColumn("consumer_e_price_EUR_per_kWh");
//		TimeSeriesProfile consumer_e_price = toTimeSeriesProfile(tariffs, "consumer_e_price_EUR_per_kWh");
//		System.out.println(consumer_e_price);
		
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulation sim = new EssimSimulation();
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1 ,1, 0,0,0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2020, 1 ,1, 0,0,0)));
		
		Market market = new Market();
		sim.setMarket(market);
		market.setMinPrice(-100.0);
		market.setMaxPrice(100.0);
		market.setDeltaPrice(1E-12);
		
		sim.setScenarioID("testcase1-realpriceprofile");
		String simId = mongo.addSimulation(sim);
		// TODO: what is the goal of this ESDL. Prices seem to be attached to the demand...?
		String esdl = new String(DynamicPricesTest.class.getResourceAsStream("/ED-Imp-Exp-highprices.esdl").readAllBytes(), StandardCharsets.UTF_8);
		sim.setInfluxURL("http://localhost:8086");
		
		ESSimEngine essimEngine = new ESSimEngine("local-realprices-test", simId, sim, esdl, mongo);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
		essimEngine.getObservationManager().registerConsumer(inMemoryObservationConsumer);
		
		
		essimEngine.startSimulation();
		while (mongo.getSimulationStatus(simId).getState() != Status.COMPLETE) {
			Thread.sleep(100);
		}
		
		Thread.sleep(1000);
		String grafanaDashboard = essimEngine.createGrafanaDashboard();
		
		
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
		

		for (Entry<String, Table> entry : tables.entrySet()) {			
			Table table = entry.getValue();
			double total = table.doubleColumn("allocationEnergy").sum();
			System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
			Resource eResource = es.eResource();
			EnergyAsset asset = (EnergyAsset)eResource.getEObject(entry.getKey());
			if (asset instanceof Export) {
				assertEquals(total, 0.0, 1E-3);
			} else if (asset instanceof ElectricityDemand || asset instanceof ElectricityNetwork) {
				assertEquals(total, 1.0E9, 1E6);
			} else if (asset instanceof Import) {
				assertEquals(-1.05E9, total, 1E6);
			}
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
