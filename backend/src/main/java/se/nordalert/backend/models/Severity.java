package se.nordalert.backend.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
  INFO("info", 0),
  LOW("low", 1),
  MEDIUM("medium", 2),
  HIGH("high", 3);

  private final String value;
  private final int rank;

  Severity(String value, int rank) {
    this.value = value;
    this.rank = rank;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public int rank() {
    return rank;
  }

  public static Severity fromQuery(String value) {
    for (Severity severity : values()) {
      if (severity.value.equalsIgnoreCase(value)) {
        return severity;
      }
    }
    throw new IllegalArgumentException("Unsupported severity: " + value);
  }
}
