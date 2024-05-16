package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(
    as = MSOConfigurationImpl.class
)
public interface MSOConfiguration {
  @JsonProperty("mqttHost")
  String getMqttHost();

  @JsonProperty("mqttHost")
  void setMqttHost(String mqttHost);

  @JsonProperty("mqttPort")
  int getMqttPort();

  @JsonProperty("mqttPort")
  void setMqttPort(int mqttPort);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
