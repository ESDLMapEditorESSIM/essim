package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "networkHTMLDiag"
})
public class TransportNetwork {
  @JsonProperty("name")
  private String name;

  @JsonProperty("networkHTMLDiag")
  private String networkHTMLDiag;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("name")
  public String getName() {
    return this.name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("networkHTMLDiag")
  public String getNetworkHTMLDiag() {
    return this.networkHTMLDiag;
  }

  @JsonProperty("networkHTMLDiag")
  public void setNetworkHTMLDiag(String networkHTMLDiag) {
    this.networkHTMLDiag = networkHTMLDiag;
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
