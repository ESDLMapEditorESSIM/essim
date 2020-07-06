package nl.tno.essim.observation;


public interface IObservationProvider {
	
	void setObservationManager(IObservationManager manager);

	String getProviderName();

	String getProviderType();

}
