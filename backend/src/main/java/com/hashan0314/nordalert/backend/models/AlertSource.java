package com.hashan0314.nordalert.backend.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertSource {
  POLISEN("polisen"),
  SMHI("smhi"),
  KRISINFORMATION("krisinformation");

  private final String value;

  AlertSource(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public static AlertSource fromValue(String value) {
    for (AlertSource source : values()) {
      if (source.value.equalsIgnoreCase(value)) {
        return source;
      }
    }
    throw new IllegalArgumentException("Unsupported source: " + value);
  }
}
