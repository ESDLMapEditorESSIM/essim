package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(
    as = MarketImpl.class
)
public interface Market {
  @JsonProperty(
      value = "minPrice",
      defaultValue = "0.0"
  )
  Number getMinPrice();

  @JsonProperty(
      value = "minPrice",
      defaultValue = "0.0"
  )
  void setMinPrice(Number minPrice);

  @JsonProperty(
      value = "maxPrice",
      defaultValue = "1.0"
  )
  Number getMaxPrice();

  @JsonProperty(
      value = "maxPrice",
      defaultValue = "1.0"
  )
  void setMaxPrice(Number maxPrice);

  @JsonProperty(
      value = "deltaPrice",
      defaultValue = "0.000001"
  )
  Number getDeltaPrice();

  @JsonProperty(
      value = "deltaPrice",
      defaultValue = "0.000001"
  )
  void setDeltaPrice(Number deltaPrice);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
