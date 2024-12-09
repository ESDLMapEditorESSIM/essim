package nl.tno.essim.tests;

import esdl.Asset;
import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.Port;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.model.Market;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.time.EssimTime;
import org.eclipse.emf.common.util.EList;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoadflowTest {

	static HashMap<String, List<String>> expectedCurves;
	private static AtomicInteger counter = new AtomicInteger(1);


	static EnergySystem runTest(String esdl, Market market,
			InMemoryObservationConsumer inMemoryObservationConsumer) throws Exception {
		IMongoBackend mongo = new MongoBackendStub();
		EssimSimulation sim = new EssimSimulation();
		sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
		sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));
		sim.setScenarioID(LoadflowTest.class.getSimpleName());
		sim.setPowsyblLoadFlow(true);
		if (System.getenv("INFLUX_URL") != null) {
			sim.setInfluxURL(System.getenv("INFLUX_URL"));
		}
		if (market != null) {
			sim.setMarket(market);
		}

		String simId = mongo.addSimulation(sim);
		ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, esdl, mongo);
		essimEngine.getSimulationManager().addObservationConsumer(inMemoryObservationConsumer);
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
	void testLoadFlow() throws Exception {
		String esdl = new String(LoadflowTest.class.getResourceAsStream("/Loadflow-1.esdl").readAllBytes(), StandardCharsets.UTF_8);
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = runTest(esdl, null, inMemoryObservationConsumer);

		String resultESDL = ESSimEngine.ecoreResourceToString(energySystem);
		System.out.println("Resulting ESDL: \n" + resultESDL);
	}
}
