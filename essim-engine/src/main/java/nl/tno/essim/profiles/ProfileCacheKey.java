package nl.tno.essim.profiles;

public record ProfileCacheKey(String uri, String databaseOrSchema, String measurement, String field, String filters,
							  Double annualChangePercentage) {
}
