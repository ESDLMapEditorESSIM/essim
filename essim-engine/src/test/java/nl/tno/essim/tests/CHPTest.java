package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import esdl.EnergyAsset;
import esdl.EnergySystem;
import esdl.impl.AbstractBasicConversionImpl;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

public class CHPTest {

	private void checkResultsDBD(InMemoryObservationConsumer inMemoryObservationConsumer, EnergySystem es,
			Map<String, Double> expectedEnergy) {
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
//			table.stringColumn(null)
			double total = table.doubleColumn("allocationEnergy").sum();
			System.out.println(
					"- " + table.stringColumn("assetName").get(0) + ": " + total + " (" + (total / 3600000.0) + "kWh)");
			Resource eResource = es.eResource();
			EnergyAsset asset = (EnergyAsset) eResource.getEObject(entry.getKey());
//			if (asset instanceof AbstractBasicConversionImpl) {
//				Column<?> column = table.column("bidcurve");
//				int i = 0;
//				for (Object row : column.asList()) {
//					System.out.println("Bidcurve " + i + " " + asset.getName() + ": " + row);
//					i++;
//				}
//			}
			if (expectedEnergy.containsKey(asset.getName())) {
//				System.out.println("Testing " + asset.getName() + " : " + expectedEnergy.get(asset.getName()));
				assertEquals(expectedEnergy.get(asset.getName()), total, 1E3, "Testing " + asset.getName());
			}
		}
	}

	@Test
	void testConversion() throws Exception {
		File esdlFile = new File("src/test/resources/CHP-test2.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = ConversionTest.runTest(esdlFile, null, inMemoryObservationConsumer);
		ImmutableMap<String, Double> expected = ImmutableMap.<String, Double>builder().put("CHP_620e", 50.0 * 3600000)
				.put("Import_05f5", -4.0 * 3600000).put("Import_7e31", -500.0 * 3600000)
				.put("ElectricityDemand_0601", 46.0 * 3600000).put("Export_e6de", 208.0 * 3600000).build();
		checkResultsDBD(inMemoryObservationConsumer, energySystem, expected);
	}

	@Test
	void testConversion2() throws Exception {
		File esdlFile = new File("src/test/resources/CHP-test3.esdl");
		InMemoryObservationConsumer inMemoryObservationConsumer = new InMemoryObservationConsumer();
		EnergySystem energySystem = ConversionTest.runTest(esdlFile, null, inMemoryObservationConsumer);
		ImmutableMap<String, Double> expected = ImmutableMap.<String, Double>builder()
				.put("CHP_620e", 9.2 * 3600000)
				.put("Import_05f5", 0.0)
				.put("HeatingDemand_9f58", 200.0*3600000)
				.put("ElectricityDemand_0601", 46.0*3600000)
				.put("Import_7e31", -92.0 * 3600000)
				.put("Import_7e31", -92.0 * 3600000)
				.put("Export_e6de", 208.0 * 3600000).build();
		checkResultsDBD(inMemoryObservationConsumer, energySystem, expected);
	}

}
