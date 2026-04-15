package nl.tno.essim.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import esdl.Area;
import esdl.DoubleKPI;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.Instance;
import esdl.KPI;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.kpi.KPIResultProcessor;
import nl.tno.essim.model.KPIModule;
import nl.tno.essim.utils.ESDLUtil;

public class KPIModuleOutputTest {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	 @Test
	 void testKPIOutputTotalAbsoluteEnergyImbalanceID() throws Exception {
		 
		 //String string = Files.readString(Paths.get("/src/test/resource/kpis/TotalAbsoluteEnergyImbalanceID.json"));
		 String string = new String(KPIModuleOutputTest.class.getResourceAsStream("/kpis/TotalAbsoluteEnergyImbalanceID.json").readAllBytes(), StandardCharsets.UTF_8);
		 Object kpiOutput = MAPPER.readValue(string, Object.class);	 
		 EnergySystem es = EsdlFactory.eINSTANCE.createEnergySystem();
		 Instance instance = EsdlFactory.eINSTANCE.createInstance();
		 instance.setName("Main instance");
		 Area area = EsdlFactory.eINSTANCE.createArea();
		 area.setName("Main area for KPIs");
		 es.getInstance().add(instance);
		 instance.setArea(area);
		 es.setName("KPI test");
		 
		 KPIModule m = new KPIModule();
		 m.setId("TotalAbsoluteEnergyImbalanceID");
		 m.setResult(kpiOutput);
		 
		 ObjectNode objectNode = MAPPER.createObjectNode();
		 JsonNode result = ESSimEngine.getObjectMapper().valueToTree(kpiOutput);
	     objectNode.set(m.getId(), result);
		 
		 KPIResultProcessor.convertKPIJsonToESDLKPIs(es, m, result);
		 System.out.println(ESDLUtil.ecoreResourceToString(es));
		 
		 assertNotNull(area.getKPIs());
		 assertEquals(area.getKPIs().getKpi().size(), 6);
		 KPI kpiResult = area.getKPIs().getKpi().getFirst();
		 assertTrue(kpiResult instanceof DoubleKPI);
		 DoubleKPI doubleKPI = (DoubleKPI) kpiResult;
		 assertEquals(doubleKPI.getValue(), 4.0000000000008E15d);
		 
	 }
	 
	 @Test
	 void testKPIOutputTotalEnergyDemandID() throws Exception {
		 
		 //String string = Files.readString(Paths.get("/src/test/resource/kpis/TotalAbsoluteEnergyImbalanceID.json"));
		 String string = new String(KPIModuleOutputTest.class.getResourceAsStream("/kpis/TotalEnergyDemandID.json").readAllBytes(), StandardCharsets.UTF_8);
		 Object kpiOutput = MAPPER.readValue(string, Object.class);	 
		 EnergySystem es = EsdlFactory.eINSTANCE.createEnergySystem();
		 Instance instance = EsdlFactory.eINSTANCE.createInstance();
		 instance.setName("Main instance");
		 Area area = EsdlFactory.eINSTANCE.createArea();
		 area.setName("Main area for KPIs");
		 es.getInstance().add(instance);
		 instance.setArea(area);
		 es.setName("KPI test");
		 
		 KPIModule m = new KPIModule();
		 m.setId("TotalEnergyDemandID");
		 m.setResult(kpiOutput);
		 
		 ObjectNode objectNode = MAPPER.createObjectNode();
		 JsonNode result = ESSimEngine.getObjectMapper().valueToTree(kpiOutput);
		 objectNode.set(m.getId(), result);
		 
		 KPIResultProcessor.convertKPIJsonToESDLKPIs(es, m, result);
		 System.out.println(ESDLUtil.ecoreResourceToString(es));
		 
		 assertNotNull(area.getKPIs());
		 assertEquals(area.getKPIs().getKpi().size(), 2);
		 KPI kpiResult = area.getKPIs().getKpi().getFirst();
		 assertTrue(kpiResult instanceof DoubleKPI);
		 DoubleKPI doubleKPI = (DoubleKPI) kpiResult;
		 assertEquals(doubleKPI.getValue(), 23000000000016000.0d);
		 
	 }
	 
}
