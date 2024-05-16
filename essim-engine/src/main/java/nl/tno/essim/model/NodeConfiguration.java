package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(
    as = NodeConfigurationImpl.class
)
public interface NodeConfiguration {
  @JsonProperty("esdlNodeId")
  String getEsdlNodeId();

  @JsonProperty("esdlNodeId")
  void setEsdlNodeId(String esdlNodeId);

  @JsonProperty("config")
  Object getConfig();

  @JsonProperty("config")
  void setConfig(Object config);

  @JsonProperty("mqttHost")
  String getMqttHost();

  @JsonProperty("mqttHost")
  void setMqttHost(String mqttHost);

  @JsonProperty("mqttPort")
  int getMqttPort();

  @JsonProperty("mqttPort")
  void setMqttPort(int mqttPort);

  @JsonProperty("mqttTopic")
  String getMqttTopic();

  @JsonProperty("mqttTopic")
  void setMqttTopic(String mqttTopic);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
