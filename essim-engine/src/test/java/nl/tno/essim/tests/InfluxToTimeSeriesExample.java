package nl.tno.essim.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLInfoImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLMapImpl;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import nl.tno.essim.profiles.EssimQuery;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.TimeSeriesProfile;
import esdl.util.EsdlResourceFactoryImpl;
import esdl.util.EsdlResourceImpl;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.time.EssimTime;
@Slf4j
public class InfluxToTimeSeriesExample {

	String INFLUX_URL = "http://localhost:8086";
	String querySQL = "SELECT \"E1A\" FROM \"standard_profiles_comma\" WHERE time >= '2018-12-31T23:00:00Z' AND time <= '2019-12-31T23:00:00Z'";
	String database = "energy_profiles";

	public InfluxToTimeSeriesExample() throws IOException {
		System.out.println("Reading from influx: " + INFLUX_URL + ": " + querySQL);
		InfluxDB influxClient = InfluxDBFactory.connect(INFLUX_URL).enableGzip();
		
		EssimQuery essimQuery = new EssimQuery(querySQL, database);
		QueryResult queryResult = influxClient.query(essimQuery);
		if (queryResult.hasError() || queryResult.getResults() == null) {
			throw new IllegalArgumentException("Error querying InfluxDB for query " + essimQuery.getCommand()
					+ ", queryError: " + queryResult.getError());
		}
		Result result = queryResult.getResults().get(0);
		TimeSeriesProfile timeSeriesProfile = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
		Series series = result.getSeries().get(0); 
		timeSeriesProfile.setName("data from " + database + ": " + series.getName());
		LocalDateTime startTime = LocalDateTime.parse((String) series.getValues().get(0).get(0),DateTimeFormatter.ISO_DATE_TIME);
		LocalDateTime secondTime = LocalDateTime.parse((String) series.getValues().get(1).get(0),DateTimeFormatter.ISO_DATE_TIME);
		long timeBetweenSeconds = ChronoUnit.SECONDS.between(startTime, secondTime);
		timeSeriesProfile.setStartDateTime(EssimTime.localDateTimeToDate(secondTime));
		timeSeriesProfile.setTimestep((int)timeBetweenSeconds);
		log.debug("Query returned {} values", series.getValues().size());
		for (List<Object> entry: series.getValues()) {
			//LocalDateTime datetime = LocalDateTime.parse((String) entry.get(0),DateTimeFormatter.ISO_DATE_TIME);
			Double value = (Double)entry.get(1); // value
			timeSeriesProfile.getValues().add(value);
			//System.out.println(value);
		}
		System.out.println(timeSeriesProfile);
		
		//EsdlResourceImpl esdlResource = new EsdlResourceImpl(null);
		Resource esdlResource = new EsdlResourceFactoryImpl().createResource(URI.createURI("timeseries.esdl"));
		esdlResource.getContents().add(timeSeriesProfile);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
        options.put(XMIResource.OPTION_ENCODING, "UTF-8");
		esdlResource.save(baos, options);
		String esdl = baos.toString(StandardCharsets.UTF_8);
		//System.out.println(esdl);
		
		
		
		TimeSeriesProfile timeSeriesProfile2 = EsdlFactory.eINSTANCE.createTimeSeriesProfile();
		timeSeriesProfile2.setName("Test");
		timeSeriesProfile2.setStartDateTime(new Date());
		timeSeriesProfile2.getValues().add(1.0);
		timeSeriesProfile2.getValues().add(2.0);
		
		EsdlResourceImpl esdlResource2 = new EsdlResourceImpl(URI.createURI("timeseries2.esdl"));
		esdlResource2.getContents().add(timeSeriesProfile2);
		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		EAttribute timeSeriesProfile_startDateTime = EsdlPackage.eINSTANCE.getTimeSeriesProfile_StartDateTime();
		EAttribute timeSeriesProfile_Values = EsdlPackage.eINSTANCE.getTimeSeriesProfile_Values();
		XMLMapImpl xmlMap = new XMLMapImpl();
		XMLInfoImpl startDateTimeXMLInfo = new XMLInfoImpl();
		startDateTimeXMLInfo.setXMLRepresentation(XMLInfoImpl.ELEMENT);
		XMLInfoImpl valuesXMLInfo = new XMLInfoImpl();
		valuesXMLInfo.setXMLRepresentation(XMLInfoImpl.CONTENT);
		valuesXMLInfo.setName("v");
		xmlMap.add(timeSeriesProfile_startDateTime, startDateTimeXMLInfo);
		xmlMap.add(timeSeriesProfile_Values, valuesXMLInfo);
		options.put(XMLResource.OPTION_XML_MAP, xmlMap);

        esdlResource2.save(baos2, options);
		String esdl2 = baos2.toString(StandardCharsets.UTF_8);
		System.out.println(esdl2);
	}

	public static void main(String[] args) throws IOException {
		new InfluxToTimeSeriesExample();

	}

}
