package nl.tno.essim.kpi;

import java.util.UUID;

import org.eclipse.emf.common.util.EList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import esdl.AbstractQuantityAndUnit;
import esdl.Area;
import esdl.DistributionKPI;
import esdl.DoubleKPI;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.KPI;
import esdl.KPIs;
import esdl.StringItem;
import esdl.StringLabelDistribution;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.model.KPIModule;
import nl.tno.essim.utils.ESDLUtil;

@Slf4j
public class KPIResultProcessor {
	
	public static void convertKPIJsonToESDLKPIs(EnergySystem energySystem, KPIModule kpiModule, JsonNode result) {
		try {
			String kpiModuleId = kpiModule.getId();
			// JSON Object should start with ID of KPI module e.g. "TotalAbsoluteEnergyImbalanceID" 
			if (result instanceof ObjectNode o) {
				JsonNode kpiModuleMain = o.get(kpiModuleId);
				// main KPI object has "Description" (text), "scope" ["Local", "Total"], "id" (same as main id), "status" [Success, Not yet started, Calculating]
				// and "kpi" that describes the kpi values
				if (kpiModuleMain != null && kpiModuleMain instanceof ObjectNode k) {
					String description = k.get("Description").asText(); // KPI Module description
					String scope = k.get("scope").asText();  // Scope of KPI, not sure what do do with this info
					String id = k.get("id").asText();
					String status = k.get("status").asText();
					JsonNode kpiField = k.get("kpi");
					log.debug("Adding KPIs - Found KPIModule result with ID=" + id + " and status " + status);
					// only process KPIs with status == Success
					if (kpiField != null && kpiField instanceof ArrayNode kpiList && status.toLowerCase().equals("success")) {
						// Actual list of KPIs calculated by this module
						// Keys: [system, per_carrier]
						for (JsonNode highLevelItem : kpiList) {
							if (highLevelItem.has("system")) {
								// list of system KPI
								JsonNode systemKPINode = highLevelItem.get("system");
								if (systemKPINode != null && systemKPINode instanceof ArrayNode systemKPIArray) {								
									for (JsonNode subSystemKPI : systemKPIArray) {
										handleKPI(energySystem, "system", subSystemKPI);
									}
								}
							} else if (highLevelItem.has("per_carrier")) {
								// per carrier KPI
								JsonNode systemKPINode = highLevelItem.get("per_carrier");
								if (systemKPINode != null && systemKPINode instanceof ArrayNode systemKPIArray) {								
									for (JsonNode subSystemKPI : systemKPIArray) {
										handleKPI(energySystem, "per_carrier", subSystemKPI);
									}
								}
							} else if (highLevelItem.has("Name") && highLevelItem.has("Unit") && highLevelItem.has("Value")) {
								// no distinction in KPIs
								handleKPI(energySystem, "none", highLevelItem);
							} else {
							
								log.debug("Adding KPIs - > Unknown kpi type: " + highLevelItem.asText());
							}
						}
					} else if (!status.toLowerCase().equals("success")) {
						log.warn("Adding KPIs - Status of KPI with ID=" + id + " is not Success, but " + status + ", ignoring.");
					}
					
				}
				
			}
		} catch (Exception e) {
			log.error("Error parsing KPIs from KPI module output: " + e.getMessage(), e);
		}
	}

	private static void handleKPI(EnergySystem energySystem, String type, JsonNode subSystemKPI) {
		// system KPI is like:
		//		{
		//            "Values": 23000000000016000.0,
		//            "Unit": "Joule",
		//            "Name": "Total energy demand [Joule]"
		//       }
		// or per_carrier KPI like:
		//		{
		//            "Values": [
		//                {
		//                    "carrier": "Gas",
		//                    "value": 7000000000008000.0
		//                },
		//                {
		//                    "carrier": "Heat",
		//                    "value": 10000000000008000.0
		//                },
		//                {
		//                    "carrier": "Electricity",
		//                    "value": 6000000000000000.0
		//                }
		//            ],
		//            "Unit": "Joule",
		//            "Name": "Total energy demand, per energy carrier [Joule]"
		//        }		
		
		Area mainArea = energySystem.getInstance().getFirst().getArea();
		KPIs kpis = mainArea.getKPIs();
		if (kpis == null) {
			kpis = EsdlFactory.eINSTANCE.createKPIs();
			kpis.setDescription("ESSIM Energy system KPIs");
			kpis.setId(UUID.randomUUID().toString());
		}
		EList<KPI> areaKPIList = kpis.getKpi();
		
		if (subSystemKPI != null && subSystemKPI instanceof ObjectNode jsonObject) {
			String kpiName = jsonObject.get("Name").asText();
			String kpiUnitString = jsonObject.get("Unit").asText();
			JsonNode valuesNode = jsonObject.get("Values");
			if (valuesNode != null && valuesNode instanceof ArrayNode valuesArray) {
				// array of values: distribution KPI
				DistributionKPI distributionKPI = EsdlFactory.eINSTANCE.createDistributionKPI();
				distributionKPI.setName(kpiName);
				distributionKPI.setQuantityAndUnit(getUnit(kpiUnitString, energySystem));
				distributionKPI.setId(UUID.randomUUID().toString());
				StringLabelDistribution stringLabelDistribution = EsdlFactory.eINSTANCE.createStringLabelDistribution();
				stringLabelDistribution.setName(kpiName);
				distributionKPI.setDistribution(stringLabelDistribution);
				for (JsonNode subKPI : valuesArray) {
					//if (type.equals("per_carrier")) { // not sure if this is required...
						double value = subKPI.get("value").asDouble();
						String carrier = subKPI.get("carrier").asText();
						StringItem stringItem = EsdlFactory.eINSTANCE.createStringItem();
						stringItem.setLabel(carrier);
						stringItem.setValue(value);
						stringLabelDistribution.getStringItem().add(stringItem);
					//} else {
					//	log.warn("Adding KPIs - Unknown kpi type: " + type + " for KPI " + kpiName);
					//}
				}
				areaKPIList.add(distributionKPI);
				log.debug("Adding KPIs - Added KPI for " + distributionKPI.getName());
			} else if (valuesNode != null && valuesNode instanceof ValueNode singleValueNode) {
				double value = singleValueNode.asDouble();
				DoubleKPI doubleKPI = EsdlFactory.eINSTANCE.createDoubleKPI();
				doubleKPI.setName(kpiName);
				doubleKPI.setQuantityAndUnit(getUnit(kpiUnitString, energySystem));
				doubleKPI.setValue(value);
				doubleKPI.setId(UUID.randomUUID().toString());
				areaKPIList.add(doubleKPI);
				log.debug("Adding KPIs - Added KPI for " + doubleKPI.getName());
			} else {
				log.debug("Adding KPIs - Cant find value for KPI " + kpiName);
			}
		}
	}

	private static AbstractQuantityAndUnit getUnit(String kpiUnitString, EnergySystem energySystem) {
		// TODO: seems to cover all cases, but smells like a hack
		switch (kpiUnitString.toLowerCase()) {
		case "joule":
		case "j":
			return ESDLUtil.createQaUReference(ESDLUtil.getOrCreateEnergyInJouleQuantityAndUnit(energySystem));
		case "watt":
		case "w":
			return ESDLUtil.createQaUReference(ESDLUtil.getOrCreatePowerInWattQuantityAndUnit(energySystem));
		case "hour":
		case "h":
			return ESDLUtil.createQaUReference(ESDLUtil.getOrCreateTimeInHourQuantityAndUnit(energySystem));
		case "percentage":
		case "%":
			return ESDLUtil.createQaUReference(ESDLUtil.getOrCreatePercentageQuantityAndUnit(energySystem));
		}
		// default case, no QaU...
		log.warn("Adding KPIs - Cannot convert unit " + kpiUnitString + " to a suitable ESDL QuantityAndUnitType");
		return null;
	}

}
