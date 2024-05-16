package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "kafkaURL",
    "modules"
})
public class RemoteKPIModuleImpl implements RemoteKPIModule {
  @JsonProperty("kafkaURL")
  private String kafkaURL;

  @JsonProperty("modules")
  private List<KPIModule> modules;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("kafkaURL")
  public String getKafkaURL() {
    return this.kafkaURL;
  }

  @JsonProperty("kafkaURL")
  public void setKafkaURL(String kafkaURL) {
    this.kafkaURL = kafkaURL;
  }

  @JsonProperty("modules")
  public List<KPIModule> getModules() {
    return this.modules;
  }

  @JsonProperty("modules")
  public void setModules(List<KPIModule> modules) {
    this.modules = modules;
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
