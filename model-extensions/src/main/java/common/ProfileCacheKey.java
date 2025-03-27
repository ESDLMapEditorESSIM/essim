package common;

public record ProfileCacheKey(String uri, String databaseOrSchema, String measurement, String field,
							  Double annualChangePercentage) {
}
