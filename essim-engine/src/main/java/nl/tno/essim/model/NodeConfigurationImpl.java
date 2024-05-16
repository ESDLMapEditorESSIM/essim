package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "esdlNodeId",
    "config",
    "mqttHost",
    "mqttPort",
    "mqttTopic"
})
public class NodeConfigurationImpl implements NodeConfiguration {
  @JsonProperty("esdlNodeId")
  private String esdlNodeId;

  @JsonProperty("config")
  private Object config;

  @JsonProperty("mqttHost")
  private String mqttHost;

  @JsonProperty("mqttPort")
  private int mqttPort;

  @JsonProperty("mqttTopic")
  private String mqttTopic;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("esdlNodeId")
  public String getEsdlNodeId() {
    return this.esdlNodeId;
  }

  @JsonProperty("esdlNodeId")
  public void setEsdlNodeId(String esdlNodeId) {
    this.esdlNodeId = esdlNodeId;
  }

  @JsonProperty("config")
  public Object getConfig() {
    return this.config;
  }

  @JsonProperty("config")
  public void setConfig(Object config) {
    this.config = config;
  }

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

  @JsonProperty("mqttTopic")
  public String getMqttTopic() {
    return this.mqttTopic;
  }

  @JsonProperty("mqttTopic")
  public void setMqttTopic(String mqttTopic) {
    this.mqttTopic = mqttTopic;
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
