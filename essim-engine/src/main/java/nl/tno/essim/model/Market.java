package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "minPrice",
    "maxPrice",
    "deltaPrice"
})
public class Market {
  @JsonProperty(
      value = "minPrice",
      defaultValue = "0.0"
  )
  private Number minPrice;

  @JsonProperty(
      value = "maxPrice",
      defaultValue = "1.0"
  )
  private Number maxPrice;

  @JsonProperty(
      value = "deltaPrice",
      defaultValue = "0.000001"
  )
  private Number deltaPrice;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty(
      value = "minPrice",
      defaultValue = "0.0"
  )
  public Number getMinPrice() {
    return this.minPrice;
  }

  @JsonProperty(
      value = "minPrice",
      defaultValue = "0.0"
  )
  public void setMinPrice(Number minPrice) {
    this.minPrice = minPrice;
  }

  @JsonProperty(
      value = "maxPrice",
      defaultValue = "1.0"
  )
  public Number getMaxPrice() {
    return this.maxPrice;
  }

  @JsonProperty(
      value = "maxPrice",
      defaultValue = "1.0"
  )
  public void setMaxPrice(Number maxPrice) {
    this.maxPrice = maxPrice;
  }

  @JsonProperty(
      value = "deltaPrice",
      defaultValue = "0.000001"
  )
  public Number getDeltaPrice() {
    return this.deltaPrice;
  }

  @JsonProperty(
      value = "deltaPrice",
      defaultValue = "0.000001"
  )
  public void setDeltaPrice(Number deltaPrice) {
    this.deltaPrice = deltaPrice;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperties(String key, Object value) {
    this.additionalProperties.put(key, value);
  }
}
