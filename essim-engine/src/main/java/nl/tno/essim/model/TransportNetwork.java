package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(
    as = TransportNetworkImpl.class
)
public interface TransportNetwork {
  @JsonProperty("name")
  String getName();

  @JsonProperty("name")
  void setName(String name);

  @JsonProperty("networkHTMLDiag")
  String getNetworkHTMLDiag();

  @JsonProperty("networkHTMLDiag")
  void setNetworkHTMLDiag(String networkHTMLDiag);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
