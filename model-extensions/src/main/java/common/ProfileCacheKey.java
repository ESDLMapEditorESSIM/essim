package common;

import lombok.Value;

@Value
public class ProfileCacheKey {
	private String host;
	private Integer port;
	private String database;
	private String measurement;
	private String field;
	private Double annualChangePercentage;
}
