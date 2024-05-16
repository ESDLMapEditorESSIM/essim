package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
  @JsonProperty("CREATED")
  CREATED("CREATED"),

  @JsonProperty("RUNNING")
  RUNNING("RUNNING"),

  @JsonProperty("COMPLETE")
  COMPLETE("COMPLETE"),

  @JsonProperty("ERROR")
  ERROR("ERROR");

  private String name;

  Status(String name) {
    this.name = name;
  }
}
