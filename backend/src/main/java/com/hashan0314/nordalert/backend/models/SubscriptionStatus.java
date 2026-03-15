package com.hashan0314.nordalert.backend.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionStatus {
  PENDING("pending"),
  CONFIRMED("confirmed");

  private final String value;

  SubscriptionStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  public static SubscriptionStatus fromValue(String value) {
    for (SubscriptionStatus status : values()) {
      if (status.value.equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unsupported subscription status: " + value);
  }
}
