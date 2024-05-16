package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(
    as = ExternalSolverImpl.class
)
public interface ExternalSolver {
  @JsonProperty("name")
  String getName();

  @JsonProperty("name")
  void setName(String name);

  @JsonProperty("url")
  String getUrl();

  @JsonProperty("url")
  void setUrl(String url);

  @JsonProperty("orderPreference")
  OrderPreference getOrderPreference();

  @JsonProperty("orderPreference")
  void setOrderPreference(OrderPreference orderPreference);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
