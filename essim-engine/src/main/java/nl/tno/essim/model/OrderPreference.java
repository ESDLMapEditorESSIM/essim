package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OrderPreference {
  @JsonProperty("FIRST")
  FIRST("FIRST"),

  @JsonProperty("LAST")
  LAST("LAST");

  private String name;

  OrderPreference(String name) {
    this.name = name;
  }
}
