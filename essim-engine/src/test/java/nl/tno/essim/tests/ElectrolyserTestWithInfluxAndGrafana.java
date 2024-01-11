package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
import nl.tno.essim.commons.Commons;
import nl.tno.essim.model.EssimSimulationImpl;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.nodes.AbstractBasicConversionNode;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

//@Testcontainers
class ElectrolyserTestWithInfluxAndGrafana {

//	@Container
//	public static GenericContainer<?> mongo = new GenericContainer<>(DockerImageName.parse("mvertes/alpine-mongo"))
//	    .withExposedPorts(27017);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {

	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Test
	void testSimulation() {
		try {
			String[] resourceListing = Commons.getResourceListing(Commons.class, "/");
			System.out.println(Arrays.asList(resourceListing));
			// IMongoBackend mongo = new MongoBackend("localhost", "27017");
			IMongoBackend mongo = new MongoBackendStub();
			EssimSimulationImpl sim = new EssimSimulationImpl();
			sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
			sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
			sim.setScenarioID("testcase1-basictest");
			sim.setInfluxURL("http://localhost:8086");

			String simId = mongo.addSimulation(sim);
			File esdlFile = new File("src/test/resources/ABCN-Variable-Prices.esdl");
			ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, esdlFile, mongo);
			InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
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

			HashMap<String, Table> tables = inMemoryObservationConsumer.getAssets();
			System.out.println("Assets: " + tables.keySet());
			LocalDateTime start = tables.values().stream().findAny().get().dateTimeColumn(0).get(0);
			LocalDateTime stop = tables.values().stream().findAny().get().dateTimeColumn(0).last(1).get(0);
			long simHours = ChronoUnit.HOURS.between(start, stop);
			System.out.println("Totals:");
			System.out.println("Sim hours:" + simHours);
			assertEquals(24, simHours);
			System.out.println("\t(" + start + " to " + stop + ")");

			for (Entry<String, Table> entry : tables.entrySet()) {
				Table table = entry.getValue();
				double total = table.doubleColumn("allocationEnergy").sum();
				System.out.println("- " + table.stringColumn("assetName").get(0) + ": " + total);
				Resource eResource = es.eResource();
				EnergyAsset asset = (EnergyAsset) eResource.getEObject(entry.getKey());
				if (asset instanceof AbstractBasicConversionImpl) {
					Column<?> column = table.column("bidcurve");
					for (Object row : column.asList()) {
						System.out.println("Bidcurve: " + row);
					}
				}

			}

			HashMap<String, Table> networks = inMemoryObservationConsumer.getNetworks();
			for (Entry<String, Table> network : networks.entrySet()) {
				Table table = network.getValue();
				double sum = table.doubleColumn("imbalanceEnergy").sum();
				System.out.println("Network " + network.getKey() + " balance = " + sum);
				assertEquals(0.0, sum, 1.0);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

}
