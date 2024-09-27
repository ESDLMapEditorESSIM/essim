package common;

public record ProfileCacheKey(String host, Integer port, String database, String measurement, String field,
							  Double annualChangePercentage) {
}
