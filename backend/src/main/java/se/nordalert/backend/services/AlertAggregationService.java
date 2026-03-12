package se.nordalert.backend.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import se.nordalert.backend.adapters.KrisinformationAdapter;
import se.nordalert.backend.adapters.PolisenAdapter;
import se.nordalert.backend.adapters.SmhiAdapter;
import se.nordalert.backend.models.Alert;
import se.nordalert.backend.models.AlertSource;
import se.nordalert.backend.models.Severity;

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

    for (PolisenAdapter.PolisenEvent event : polisenAdapter.fetchPolisenEvents()) {
      alerts.add(new Alert(
          AlertSource.POLISEN,
          event.id(),
          event.title(),
          emptyToNull(event.summary()),
          event.location().name().isBlank() ? List.of() : List.of(event.location().name()),
          Severity.INFO,
          event.occurredAt(),
          event.url()
      ));
    }

    for (SmhiAdapter.SmhiWarning warning : smhiAdapter.fetchSmhiWarnings()) {
      alerts.add(new Alert(
          AlertSource.SMHI,
          warning.id(),
          warning.eventType(),
          emptyToNull(warning.description()),
          warning.areas(),
          mapSeverity(warning.level()),
          warning.validFrom(),
          warning.url()
      ));
    }

    for (KrisinformationAdapter.KrisinformationItem item : krisinformationAdapter.fetchKrisinformationItems()) {
      alerts.add(new Alert(
          AlertSource.KRISINFORMATION,
          item.id(),
          item.headline(),
          emptyToNull(item.preamble()),
          item.counties(),
          Severity.INFO,
          item.publishedAt(),
          item.url()
      ));
    }

    alerts.sort(Comparator.comparing(Alert::publishedAt).reversed());
    return alerts;
  }

  private static Severity mapSeverity(SmhiAdapter.WarningLevel level) {
    return switch (level) {
      case RED -> Severity.HIGH;
      case ORANGE -> Severity.MEDIUM;
      case YELLOW -> Severity.LOW;
    };
  }

  private static String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
