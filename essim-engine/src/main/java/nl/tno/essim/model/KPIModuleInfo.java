package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "calculator_id",
    "title",
    "description",
    "config_config",
    "sample_output",
    "kafka_consumer_group",
    "kafka_client",
    "status",
    "up_since"
})
public class KPIModuleInfo {
  @JsonProperty("calculator_id")
  private String calculatorId;

  @JsonProperty("title")
  private String title;

  @JsonProperty("description")
  private String description;

  @JsonProperty("config_config")
  private Object configConfig;

  @JsonProperty("sample_output")
  private String sampleOutput;

  @JsonProperty("kafka_consumer_group")
  private String kafkaConsumerGroup;

  @JsonProperty("kafka_client")
  private String kafkaClient;

  @JsonProperty("status")
  private String status;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
  )
  @JsonDeserialize(
      using = TimestampDeserializer.class
  )
  @JsonProperty("up_since")
  private Date upSince;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("calculator_id")
  public String getCalculatorId() {
    return this.calculatorId;
  }

  @JsonProperty("calculator_id")
  public void setCalculatorId(String calculatorId) {
    this.calculatorId = calculatorId;
  }

  @JsonProperty("title")
  public String getTitle() {
    return this.title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  @JsonProperty("description")
  public String getDescription() {
    return this.description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("config_config")
  public Object getConfigConfig() {
    return this.configConfig;
  }

  @JsonProperty("config_config")
  public void setConfigConfig(Object configConfig) {
    this.configConfig = configConfig;
  }

  @JsonProperty("sample_output")
  public String getSampleOutput() {
    return this.sampleOutput;
  }

  @JsonProperty("sample_output")
  public void setSampleOutput(String sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  @JsonProperty("kafka_consumer_group")
  public String getKafkaConsumerGroup() {
    return this.kafkaConsumerGroup;
  }

  @JsonProperty("kafka_consumer_group")
  public void setKafkaConsumerGroup(String kafkaConsumerGroup) {
    this.kafkaConsumerGroup = kafkaConsumerGroup;
  }

  @JsonProperty("kafka_client")
  public String getKafkaClient() {
    return this.kafkaClient;
  }

  @JsonProperty("kafka_client")
  public void setKafkaClient(String kafkaClient) {
    this.kafkaClient = kafkaClient;
  }

  @JsonProperty("status")
  public String getStatus() {
    return this.status;
  }

  @JsonProperty("status")
  public void setStatus(String status) {
    this.status = status;
  }

  @JsonProperty("up_since")
  public Date getUpSince() {
    return this.upSince;
  }

  @JsonProperty("up_since")
  public void setUpSince(Date upSince) {
    this.upSince = upSince;
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
