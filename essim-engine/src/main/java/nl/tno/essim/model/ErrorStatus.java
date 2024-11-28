package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status",
    "description"
})
@RequiredArgsConstructor
public class ErrorStatus {
  @JsonProperty("status")
  private Status status;

  @JsonProperty("description")
  private String description;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("status")
  public Status getStatus() {
    return this.status;
  }

  @JsonProperty("status")
  public void setStatus(Status status) {
    this.status = status;
  }

  @JsonProperty("description")
  public String getDescription() {
    return this.description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperties(String key, Object value) {
    this.additionalProperties.put(key, value);
  }

  public ErrorStatus(Status status, String description) {
    this.status = status;
    this.description = description;
  }
}
