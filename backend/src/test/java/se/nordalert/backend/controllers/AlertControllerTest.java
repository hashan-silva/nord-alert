package se.nordalert.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import se.nordalert.backend.models.Alert;
import se.nordalert.backend.models.AlertSource;
import se.nordalert.backend.models.Severity;
import se.nordalert.backend.services.AlertAggregationService;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

  private static final String STOCKHOLM_COUNTY = "Stockholms län";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AlertAggregationService alertAggregationService;

  @Test
  void shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void shouldFilterAlertsByCountyAndSeverity() throws Exception {
    when(alertAggregationService.fetchAllAlerts()).thenReturn(List.of(
        alert("1", STOCKHOLM_COUNTY, Severity.INFO, Instant.parse("2026-03-12T16:52:18Z")),
        alert("2", STOCKHOLM_COUNTY, Severity.HIGH, Instant.parse("2026-03-12T17:52:18Z")),
        alert("3", "Skåne län", Severity.HIGH, Instant.parse("2026-03-12T18:52:18Z"))
    ));

    mockMvc.perform(get("/alerts")
            .param("county", STOCKHOLM_COUNTY)
            .param("severity", "medium"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("2"))
        .andExpect(jsonPath("$[0].severity").value("high"));
  }

  @Test
  void shouldRejectInvalidSeverity() throws Exception {
    when(alertAggregationService.fetchAllAlerts()).thenReturn(List.of());

    mockMvc.perform(get("/alerts").param("severity", "critical"))
        .andExpect(status().isBadRequest());
  }

  private static Alert alert(String id, String county, Severity severity, Instant publishedAt) {
    return new Alert(
        AlertSource.POLISEN,
        id,
        "Headline " + id,
        "Description " + id,
        List.of(county),
        severity,
        publishedAt,
        "https://example.com/" + id
    );
  }
}
