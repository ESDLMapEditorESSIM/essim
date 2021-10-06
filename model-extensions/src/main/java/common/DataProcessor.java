
package common;

import nl.tno.essim.util.Converter;

public class DataProcessor {
	private Object profileType;
	private double multiplier;

	public DataProcessor(Object profileType, double multiplier) {
		this.profileType = profileType;
		this.multiplier = multiplier;
	}

	public double process(double data) {
		return Converter.toStandardizedUnits(data * multiplier, profileType);
	}
}
