package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

@JsonDeserialize(
    as = RemoteKPIModuleImpl.class
)
public interface RemoteKPIModule {
  @JsonProperty("kafkaURL")
  String getKafkaURL();

  @JsonProperty("kafkaURL")
  void setKafkaURL(String kafkaURL);

  @JsonProperty("modules")
  List<KPIModule> getModules();

  @JsonProperty("modules")
  void setModules(List<KPIModule> modules);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
