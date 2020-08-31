/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

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
