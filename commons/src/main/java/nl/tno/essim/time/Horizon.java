package nl.tno.essim.time;

import java.time.LocalDateTime;

import lombok.Value;

/**
 * 
 * 
 * Contains a startTime and a duration
 *
 * @author werkmane
 */
@Value
public class Horizon {
	LocalDateTime startTime;
	EssimDuration period;

	public LocalDateTime getEndTime() {
		return startTime.plus(period.getAmount(), period.getUnit());
	}
}
