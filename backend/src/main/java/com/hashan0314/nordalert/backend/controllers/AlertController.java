package com.hashan0314.nordalert.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.hashan0314.nordalert.backend.models.Alert;
import com.hashan0314.nordalert.backend.models.HealthResponse;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.services.AlertAggregationService;

@RestController
@Tag(name = "Alerts", description = "Alert feed and health endpoints")
public class AlertController {

  private final AlertAggregationService alertAggregationService;

  public AlertController(AlertAggregationService alertAggregationService) {
    this.alertAggregationService = alertAggregationService;
  }

  @GetMapping("/health")
  @Operation(
      summary = "Health check",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Backend is healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
        )
      }
  )
  public HealthResponse health() {
    return new HealthResponse("ok");
  }

  @GetMapping("/alerts")
  @Operation(
      summary = "List alerts",
      description = "Returns the aggregated alert feed with optional county and severity filters.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Aggregated alerts",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Alert.class)))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid severity filter")
      }
  )
  public List<Alert> alerts(
      @Parameter(description = "Exact county name filters", example = "Stockholms län")
      @RequestParam(required = false) List<String> county,
      @Parameter(description = "Minimum severity threshold", example = "medium")
      @RequestParam(required = false) String severity
  ) {
    List<String> counties = normalizeCounties(county);
    Severity threshold = parseSeverity(severity);

    return alertAggregationService.fetchAllAlerts().stream()
        .filter(alert -> counties.isEmpty() || alert.areas().stream().anyMatch(counties::contains))
        .filter(alert -> threshold == null || alert.severity().rank() >= threshold.rank())
        .toList();
  }

  private static List<String> normalizeCounties(List<String> counties) {
    if (counties == null) {
      return List.of();
    }

    return counties.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
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
}
