package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;
import java.util.Map;

@JsonDeserialize(
    as = KPIModuleInfoImpl.class
)
public interface KPIModuleInfo {
  @JsonProperty("calculator_id")
  String getCalculatorId();

  @JsonProperty("calculator_id")
  void setCalculatorId(String calculatorId);

  @JsonProperty("title")
  String getTitle();

  @JsonProperty("title")
  void setTitle(String title);

  @JsonProperty("description")
  String getDescription();

  @JsonProperty("description")
  void setDescription(String description);

  @JsonProperty("config_config")
  Object getConfigConfig();

  @JsonProperty("config_config")
  void setConfigConfig(Object configConfig);

  @JsonProperty("sample_output")
  String getSampleOutput();

  @JsonProperty("sample_output")
  void setSampleOutput(String sampleOutput);

  @JsonProperty("kafka_consumer_group")
  String getKafkaConsumerGroup();

  @JsonProperty("kafka_consumer_group")
  void setKafkaConsumerGroup(String kafkaConsumerGroup);

  @JsonProperty("kafka_client")
  String getKafkaClient();

  @JsonProperty("kafka_client")
  void setKafkaClient(String kafkaClient);

  @JsonProperty("status")
  String getStatus();

  @JsonProperty("status")
  void setStatus(String status);

  @JsonProperty("up_since")
  Date getUpSince();

  @JsonProperty("up_since")
  void setUpSince(Date upSince);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
