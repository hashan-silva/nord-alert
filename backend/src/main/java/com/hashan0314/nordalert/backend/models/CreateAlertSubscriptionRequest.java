package com.hashan0314.nordalert.backend.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateAlertSubscriptionRequest {

  @Email
  @NotBlank
  private String email;

  private List<String> counties = List.of();
  private String severity;
  private List<String> sources = List.of();

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<String> getCounties() {
    return counties;
  }

  public void setCounties(List<String> counties) {
    this.counties = counties;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public List<String> getSources() {
    return sources;
  }

  public void setSources(List<String> sources) {
    this.sources = sources;
  }
}
