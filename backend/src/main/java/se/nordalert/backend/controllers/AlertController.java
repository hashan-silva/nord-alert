package se.nordalert.backend.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.nordalert.backend.models.Alert;
import se.nordalert.backend.models.Severity;
import se.nordalert.backend.services.AlertAggregationService;

@RestController
public class AlertController {

  private final AlertAggregationService alertAggregationService;

  public AlertController(AlertAggregationService alertAggregationService) {
    this.alertAggregationService = alertAggregationService;
  }

  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("ok");
  }

  @GetMapping("/alerts")
  public List<Alert> alerts(
      @RequestParam(required = false) String county,
      @RequestParam(required = false) String severity
  ) {
    Severity threshold = parseSeverity(severity);

    return alertAggregationService.fetchAllAlerts().stream()
        .filter(alert -> county == null || alert.areas().contains(county))
        .filter(alert -> threshold == null || alert.severity().rank() >= threshold.rank())
        .toList();
  }

  private static Severity parseSeverity(String severity) {
    if (severity == null || severity.isBlank()) {
      return null;
    }

    try {
      return Severity.fromQuery(severity);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  @ResponseStatus(HttpStatus.OK)
  public record HealthResponse(String status) {
  }
}
