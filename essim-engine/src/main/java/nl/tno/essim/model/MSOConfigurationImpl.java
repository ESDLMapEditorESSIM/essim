package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "mqttHost",
    "mqttPort"
})
public class MSOConfigurationImpl implements MSOConfiguration {
  @JsonProperty("mqttHost")
  private String mqttHost;

  @JsonProperty("mqttPort")
  private int mqttPort;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("mqttHost")
  public String getMqttHost() {
    return this.mqttHost;
  }

  @JsonProperty("mqttHost")
  public void setMqttHost(String mqttHost) {
    this.mqttHost = mqttHost;
  }

  @JsonProperty("mqttPort")
  public int getMqttPort() {
    return this.mqttPort;
  }

  @JsonProperty("mqttPort")
  public void setMqttPort(int mqttPort) {
    this.mqttPort = mqttPort;
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
