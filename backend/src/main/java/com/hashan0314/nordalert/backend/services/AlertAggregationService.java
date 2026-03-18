package com.hashan0314.nordalert.backend.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.hashan0314.nordalert.backend.adapters.KrisinformationAdapter;
import com.hashan0314.nordalert.backend.adapters.PolisenAdapter;
import com.hashan0314.nordalert.backend.adapters.SmhiAdapter;
import com.hashan0314.nordalert.backend.models.Alert;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.KrisinformationItem;
import com.hashan0314.nordalert.backend.models.PolisenEvent;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SmhiWarning;
import com.hashan0314.nordalert.backend.models.SmhiWarningLevel;

@Service
public class AlertAggregationService {

  private final PolisenAdapter polisenAdapter;
  private final SmhiAdapter smhiAdapter;
  private final KrisinformationAdapter krisinformationAdapter;

  public AlertAggregationService(
      PolisenAdapter polisenAdapter,
      SmhiAdapter smhiAdapter,
      KrisinformationAdapter krisinformationAdapter
  ) {
    this.polisenAdapter = polisenAdapter;
    this.smhiAdapter = smhiAdapter;
    this.krisinformationAdapter = krisinformationAdapter;
  }

  public List<Alert> fetchAllAlerts() {
    List<Alert> alerts = new ArrayList<>();

    for (PolisenEvent event : polisenAdapter.fetchPolisenEvents()) {
      alerts.add(new Alert(
          AlertSource.POLISEN,
          event.id(),
          event.title(),
          emptyToNull(event.summary()),
          event.location().name().isBlank() ? List.of() : List.of(event.location().name()),
          Severity.INFO,
          event.occurredAt(),
          event.url(),
          event.location().lat(),
          event.location().lon(),
          null
      ));
    }

    for (SmhiWarning warning : smhiAdapter.fetchSmhiWarnings()) {
      alerts.add(new Alert(
          AlertSource.SMHI,
          warning.id(),
          warning.eventType(),
          emptyToNull(warning.description()),
          warning.areas(),
          mapSeverity(warning.level()),
          warning.validFrom(),
          warning.url(),
          null,
          null,
          warning.geoJson()
      ));
    }

    for (KrisinformationItem item : krisinformationAdapter.fetchKrisinformationItems()) {
      alerts.add(new Alert(
          AlertSource.KRISINFORMATION,
          item.id(),
          item.headline(),
          emptyToNull(krisinformationDescription(item)),
          item.counties(),
          Severity.INFO,
          item.publishedAt(),
          item.url(),
          null,
          null,
          null
      ));
    }

    alerts.sort(Comparator.comparing(Alert::publishedAt).reversed());
    return alerts;
  }

  private static Severity mapSeverity(SmhiWarningLevel level) {
    return switch (level) {
      case RED -> Severity.HIGH;
      case ORANGE -> Severity.MEDIUM;
      case YELLOW -> Severity.LOW;
    };
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static String krisinformationDescription(KrisinformationItem item) {
    LinkedHashSet<String> parts = new LinkedHashSet<>();
    addIfPresent(parts, item.bodyText());
    addIfPresent(parts, item.preamble());
    addIfPresent(parts, item.pushMessage());
    return String.join("\n\n", parts);
  }

  private static void addIfPresent(LinkedHashSet<String> parts, String value) {
    if (value != null) {
      String normalized = value.trim();
      if (!normalized.isBlank()) {
        parts.add(normalized);
      }
    }
  }
}
