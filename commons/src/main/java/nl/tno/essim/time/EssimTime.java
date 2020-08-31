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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import lombok.Data;

/**
 * @author Ewoud Werkman <ewoud.werkman@tno.nl>
 *
 */
@Data
public class EssimTime {

	public static ZoneId defaultTimeZone = ZoneId.of("UTC");
	public static EssimDuration defaultSimulationStepLength = EssimDuration.of(1l, ChronoUnit.DAYS);
	private final LocalDateTime simulationStartTime;
	private final LocalDateTime simulationEndTime;
	private final EssimDuration simulationStepLength;
	private LocalDateTime time;
	private LocalDateTime lastDecisionTime;
	private boolean makeDecision = false;

	public EssimTime(LocalDateTime simulationStartTime, LocalDateTime simulationEndTime,
			EssimDuration simulationStepLength) {
		this.simulationStartTime = simulationStartTime;
		this.simulationEndTime = simulationEndTime;
		this.simulationStepLength = simulationStepLength;

		time = LocalDateTime.of(simulationStartTime.toLocalDate(), simulationStartTime.toLocalTime());
		lastDecisionTime = LocalDateTime.of(simulationStartTime.toLocalDate(), simulationStartTime.toLocalTime());
	}

	public EssimTime(LocalDateTime simulationStartTime, LocalDateTime simulationEndTime) {
		this(simulationStartTime, simulationEndTime, defaultSimulationStepLength);
	}

	public EssimTime nextTimeStep() {
		time = calculateNextLocalDateTime(time, simulationStepLength);
		return this;
	}

	public int toDiscreteSimulationTime() {
		return toDiscreteTime(simulationStepLength);
	}

	public int toDiscreteTime(EssimDuration duration) {
		// if (duration.getAmount() != 1) {
		// // use inprecise calculation if amount is not 1, as this calculation is based
		// on seconds
		// // and not each month has the same amount of seconds
		// return duration.getQuanta(Duration.between(simulationStartTime, time));
		// }
		// this works for months
		return Math.toIntExact(duration.getUnit().between(simulationStartTime, time) / duration.getAmount());
	}

	private static LocalDateTime calculateNextLocalDateTime(LocalDateTime input, EssimDuration simulationStepLength) {
		return input.plus(simulationStepLength.getAmount(), simulationStepLength.getUnit());
	}

	public static String toInfluxDBTimeString(Date d) {
		return toInfluxDBTimeString(LocalDateTime.ofInstant(d.toInstant(), defaultTimeZone));
	}

	public static String toInfluxDBTimeString(LocalDateTime ldt) {
		return ldt.atZone(defaultTimeZone).format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Based on system default ZoneID, so the value entering in the GUI (which is in
	 * System Default ZoneID) is converted correctly to LocalDateTime
	 * 
	 * @param d
	 * @return
	 */
	public static LocalDateTime dateToLocalDateTime(Date d) {
		return LocalDateTime.ofInstant(d.toInstant(), defaultTimeZone);
	}

	/**
	 * Date defined in the Eclipse GUI is in a specific time zone, but we assume it
	 * is the same as in UTC, the DIDO time zone, so 1-1-2018 00:00:00 will be the
	 * same in LocalDateTime
	 * 
	 * @param d input java.util.Date
	 * @return LocalDateTime of the Date in systemDefault() time zone
	 */
	public static LocalDateTime dateFromGUI(Date d) {
		return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
	}

	public static Date localDateTimeToDate(LocalDateTime ldt) {
		return Date.from(ldt.atZone(defaultTimeZone).toInstant());
	}
}
