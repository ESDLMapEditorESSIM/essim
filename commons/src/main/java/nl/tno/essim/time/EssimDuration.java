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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import lombok.Value;

/**
 * Immutable class to represent a duration with an amount and a unit. Underneath
 * also a java.time.Duration is available for conversion purposes.
 * <p>
 * It is based on int for usage with java arrays (which are integer-based)
 * 
 * @author werkmane
 *
 */
@Value
public class EssimDuration {

	private long amount;
	private ChronoUnit unit;

	public static EssimDuration of(long amount, ChronoUnit unit) {
		return new EssimDuration(amount, unit);
	}

	public EssimDuration(long amount, ChronoUnit unit) {
		this.amount = amount;
		this.unit = unit;
	}

	public int getQuanta(Duration d) {
		return Math.toIntExact(d.getSeconds() / (unit.getDuration().getSeconds() * amount));
	}

	public int getQuanta(EssimDuration d) {
		return Math.toIntExact(
				(d.getUnit().getDuration().getSeconds() * d.getAmount()) / (unit.getDuration().getSeconds() * amount));
	}

	/**
	 * Returns <i>estimated</i> length in seconds (e.g. a month is defined by
	 * SECONDS_IN_YEAR / 12!
	 * 
	 * @return
	 */
	public long getSeconds() {
		return unit.getDuration().getSeconds() * amount;
	}

	/**
	 * Returns a {@link Duration} of this DIDO duration, using estimation by seconds
	 * (e.g. Month is defined by SECONDS_IN_YEAR / 12)
	 * 
	 * @return
	 */
	public Duration toDuration() {
		return Duration.ofSeconds(getSeconds());
	}

	/**
	 * Returns the number of days for this duration, based on current time
	 * 
	 * <pre>
	 * startTime
	 * </pre>
	 * 
	 * .
	 * <p>
	 * dealing with different month lengths, e.g. January has 31 days, April only
	 * 30, etc. Created as convenience for having the number of days in the current
	 * month
	 * 
	 * @param startTime
	 * @return amount of days in
	 * 
	 *         <pre>
	 *         startTime
	 *         </pre>
	 * 
	 *         plus the duration define in this class
	 */
	public long toDays(LocalDateTime startTime) {
		if (unit.isTimeBased()) {
			return 0;
		}
		LocalDateTime end = startTime.plus(amount, unit);
		return ChronoUnit.DAYS.between(startTime, end);
	}

	/**
	 * Returns the number of unit in the specified ChronoUnit for this duration,
	 * based on current time
	 * 
	 * <pre>
	 * startTime
	 * </pre>
	 * 
	 * .
	 * <p>
	 * Created as convenience for having the number of
	 * 
	 * <pre>
	 * toChronoUnit
	 * </pre>
	 * 
	 * in the current duration
	 * 
	 * @param startTime
	 * @param toChronoUnit
	 * @return amount of days in
	 * 
	 *         <pre>
	 *         startTime
	 *         </pre>
	 * 
	 *         plus the duration define in this class
	 */
	public long toChronoUnitAmount(LocalDateTime startTime, ChronoUnit toChronoUnit) {
		LocalDateTime end = startTime.plus(amount, unit);
		return toChronoUnit.between(startTime, end);

	}

}
