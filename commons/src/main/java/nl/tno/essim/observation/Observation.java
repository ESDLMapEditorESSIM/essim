/**
 * 
 */
package nl.tno.essim.observation;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Singular;

/**
 * @author werkmane
 *
 */
@Builder
public class Observation implements IObservation {

	
	private LocalDateTime observedAt;
	@Singular
	private Map<String, Object> values;
	@Singular
	private Map<String, String> tags;
	
	/* (non-Javadoc)
	 * @see dido.observation.framework.IObservation#getValues()
	 */
	@Override
	public Map<String, Object> getValues() {
		return values;
	}

	/* (non-Javadoc)
	 * @see dido.observation.framework.IObservation#getTags()
	 */
	@Override
	public Map<String, String> getTags() {
		return tags;
	}

	/* (non-Javadoc)
	 * @see dido.observation.framework.IObservation#getObservedAt()
	 */
	@Override
	public LocalDateTime getObservedAt() {
		return observedAt;
	}

}
