package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "result",
    "config"
})
public class KPIModule {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @JsonProperty("id")
  private String id;

  @JsonProperty("result")
  private Object result;

  @JsonProperty("config")
  private Object config;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("id")
  public String getId() {
    return this.id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("result")
  public Object getResult() {
    return this.result;
  }

  @JsonProperty("result")
  public void setResult(Object result) {
    if (result instanceof String s) {
      try {
        this.result = MAPPER.readValue(s, Object.class);
      } catch (Exception e) {
        this.result = result;
      }
    } else {
      this.result = result;
    }
  }

  @JsonProperty("config")
  public Object getConfig() {
    return this.config;
  }

  @JsonProperty("config")
  public void setConfig(Object config) {
    this.config = config;
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
