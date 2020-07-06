package nl.tno.essim.observation;

import java.time.LocalDateTime;
import java.util.Map;

public interface IObservation {
	
	/**
	 * For influxDB this map contains the fields (Key, value)
	 * @return Map
	 */
	Map<String, Object> getValues();
	
	/**
	 * For influxDB this map contains the tags (Key, value)
	 * For non-influxdb observation consumers, both {@link this#getValues()} and {@link this#getTags()} are combined
	 * @return
	 */
	Map<String, String> getTags();
	

	LocalDateTime getObservedAt();

}
