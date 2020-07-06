package nl.tno.essim.commons;

import org.json.JSONArray;

import esdl.Carrier;

public interface ITransportSolver extends Simulatable {

	public String getId();
	
	public Carrier getCarrier();
	
	public boolean hasAnyTransportAsset();
	
	public JSONArray getFeatureCollection();	
}
