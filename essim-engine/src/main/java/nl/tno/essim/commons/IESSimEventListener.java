package nl.tno.essim.commons;

import java.util.List;

public interface IESSimEventListener {
	
	public void onSimulationCreate();
	
	public void onSimulationStart();
	
	public void onSimulationError(String errorDescription);
	
	public void onSimulationEnd();
	
	public void onDashboardCreate(String dashboardURL);
	
	public void onTransportNetworksCreate(List<String> networkDiagrams);
}
