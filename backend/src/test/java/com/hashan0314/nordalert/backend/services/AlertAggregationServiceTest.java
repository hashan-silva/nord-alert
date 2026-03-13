package com.hashan0314.nordalert.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.adapters.KrisinformationAdapter;
import com.hashan0314.nordalert.backend.adapters.PolisenAdapter;
import com.hashan0314.nordalert.backend.adapters.SmhiAdapter;
import com.hashan0314.nordalert.backend.models.Alert;
import com.hashan0314.nordalert.backend.models.KrisinformationItem;
import com.hashan0314.nordalert.backend.models.PolisenEvent;
import com.hashan0314.nordalert.backend.models.PolisenLocation;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SmhiWarning;
import com.hashan0314.nordalert.backend.models.SmhiWarningLevel;

@ExtendWith(MockitoExtension.class)
class AlertAggregationServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private PolisenAdapter polisenAdapter;

  @Mock
  private SmhiAdapter smhiAdapter;

  @Mock
  private KrisinformationAdapter krisinformationAdapter;

  private AlertAggregationService alertAggregationService;

  @BeforeEach
  void setUp() {
    alertAggregationService = new AlertAggregationService(polisenAdapter, smhiAdapter, krisinformationAdapter);
  }

  @Test
  void shouldAggregateAndSortAlertsFromAllSources() {
    when(polisenAdapter.fetchPolisenEvents()).thenReturn(List.of(
        new PolisenEvent(
            "p-1",
            "Police headline",
            "Brand",
            "Police summary",
            "https://example.com/p-1",
            Instant.parse("2026-03-12T16:52:18Z"),
            new PolisenLocation("Stockholms län", 59.0, 18.0)
        )
    ));
    when(smhiAdapter.fetchSmhiWarnings()).thenReturn(List.of(
        new SmhiWarning(
            "s-1",
            "Wind at sea",
            SmhiWarningLevel.RED,
            "Storm warning",
            List.of("The Belts"),
            Instant.parse("2026-03-13T09:00:00Z"),
            Instant.parse("2026-03-13T12:00:00Z"),
            "",
            OBJECT_MAPPER.createObjectNode().put("type", "Feature")
        )
    ));
    when(krisinformationAdapter.fetchKrisinformationItems()).thenReturn(List.of(
        new KrisinformationItem(
            "k-1",
            "Crisis headline",
            "Crisis preamble",
            List.of("Skåne län"),
            Instant.parse("2026-03-12T18:52:18Z"),
            "https://example.com/k-1",
            null
        )
    ));

    List<Alert> alerts = alertAggregationService.fetchAllAlerts();

    assertEquals(3, alerts.size());
    assertEquals(List.of("s-1", "k-1", "p-1"), alerts.stream().map(Alert::id).toList());
    assertEquals(Severity.HIGH, alerts.get(0).severity());
    assertEquals(59.0, alerts.get(2).latitude());
    assertEquals(18.0, alerts.get(2).longitude());
    assertEquals("Feature", alerts.get(0).geoJson().path("type").asText());
    assertEquals(List.of("Stockholms län"), alerts.get(2).areas());
  }

  @Test
  void shouldDropBlankDescriptionsWhenNormalizing() {
    when(polisenAdapter.fetchPolisenEvents()).thenReturn(List.of(
        new PolisenEvent(
            "p-blank",
            "Police headline",
            "Info",
            "   ",
            "https://example.com/p-blank",
            Instant.parse("2026-03-12T16:52:18Z"),
            new PolisenLocation("", null, null)
        )
    ));
    when(smhiAdapter.fetchSmhiWarnings()).thenReturn(List.of());
    when(krisinformationAdapter.fetchKrisinformationItems()).thenReturn(List.of());

    Alert alert = alertAggregationService.fetchAllAlerts().get(0);

    assertNull(alert.description());
    assertTrue(alert.areas().isEmpty());
  }
}
