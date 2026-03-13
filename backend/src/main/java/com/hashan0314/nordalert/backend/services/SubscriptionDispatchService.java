package com.hashan0314.nordalert.backend.services;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.hashan0314.nordalert.backend.adapters.SesEmailAdapter;
import com.hashan0314.nordalert.backend.models.Alert;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.SubscriptionDispatchResult;

@Service
public class SubscriptionDispatchService {

  private static final DateTimeFormatter EMAIL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
      .withZone(ZoneId.of("Europe/Stockholm"));

  private final AlertAggregationService alertAggregationService;
  private final SubscriptionService subscriptionService;
  private final SesEmailAdapter sesEmailAdapter;

  public SubscriptionDispatchService(
      AlertAggregationService alertAggregationService,
      SubscriptionService subscriptionService,
      SesEmailAdapter sesEmailAdapter
  ) {
    this.alertAggregationService = alertAggregationService;
    this.subscriptionService = subscriptionService;
    this.sesEmailAdapter = sesEmailAdapter;
  }

  public SubscriptionDispatchResult dispatchPendingAlerts() {
    List<Alert> alerts = alertAggregationService.fetchAllAlerts();
    int emailedSubscriptions = 0;
    List<AlertSubscription> subscriptions = subscriptionService.getSubscriptions();

    for (AlertSubscription subscription : subscriptions) {
      List<Alert> matchingAlerts = filterAlerts(subscription, alerts);
      if (matchingAlerts.isEmpty()) {
        continue;
      }

      sesEmailAdapter.sendEmail(
          subscription.email(),
          "NordAlert: " + matchingAlerts.size() + " new alert" + (matchingAlerts.size() == 1 ? "" : "s"),
          buildTextBody(subscription, matchingAlerts),
          buildHtmlBody(subscription, matchingAlerts)
      );

      Instant lastNotifiedAt = matchingAlerts.stream()
          .map(Alert::publishedAt)
          .filter(Objects::nonNull)
          .max(Comparator.naturalOrder())
          .orElseGet(Instant::now);
      subscriptionService.markNotified(subscription.id(), lastNotifiedAt);
      emailedSubscriptions++;
    }

    return new SubscriptionDispatchResult(subscriptions.size(), emailedSubscriptions);
  }

  private static List<Alert> filterAlerts(AlertSubscription subscription, List<Alert> alerts) {
    Instant threshold = subscription.lastNotifiedAt() != null ? subscription.lastNotifiedAt() : subscription.createdAt();

    return alerts.stream()
        .filter(alert -> subscription.counties().isEmpty()
            || alert.areas().stream().anyMatch(subscription.counties()::contains))
        .filter(alert -> subscription.severity() == null
            || alert.severity().rank() >= subscription.severity().rank())
        .filter(alert -> subscription.sources().isEmpty()
            || subscription.sources().contains(alert.source()))
        .filter(alert -> alert.publishedAt() != null && alert.publishedAt().isAfter(threshold))
        .sorted(Comparator.comparing(Alert::publishedAt))
        .toList();
  }

  private static String buildTextBody(AlertSubscription subscription, List<Alert> alerts) {
    StringBuilder builder = new StringBuilder();
    builder.append("NordAlert found ").append(alerts.size()).append(" new alerts for ")
        .append(subscription.email()).append(".\n\n");

    for (Alert alert : alerts) {
      builder.append("- [").append(alert.source().value()).append("] ")
          .append(alert.headline()).append('\n');
      if (!alert.areas().isEmpty()) {
        builder.append("  Counties: ").append(String.join(", ", alert.areas())).append('\n');
      }
      builder.append("  Severity: ").append(alert.severity().value()).append('\n');
      if (alert.publishedAt() != null) {
        builder.append("  Published: ").append(EMAIL_DATE_FORMATTER.format(alert.publishedAt())).append('\n');
      }
      if (alert.url() != null && !alert.url().isBlank()) {
        builder.append("  Source: ").append(alert.url()).append('\n');
      }
      builder.append('\n');
    }

    return builder.toString().trim();
  }

  private static String buildHtmlBody(AlertSubscription subscription, List<Alert> alerts) {
    StringBuilder builder = new StringBuilder();
    builder.append("<html><body style=\"font-family:Arial,sans-serif;color:#15324b;\">")
        .append("<h2>NordAlert update</h2>")
        .append("<p>")
        .append(alerts.size())
        .append(" new alerts matched the subscription for <strong>")
        .append(escapeHtml(subscription.email()))
        .append("</strong>.</p><ul>");

    for (Alert alert : alerts) {
      builder.append("<li style=\"margin-bottom:16px;\">")
          .append("<strong>[")
          .append(escapeHtml(alert.source().value()))
          .append("] ")
          .append(escapeHtml(alert.headline()))
          .append("</strong><br/>")
          .append("Severity: ")
          .append(escapeHtml(alert.severity().value()));
      if (!alert.areas().isEmpty()) {
        builder.append("<br/>Counties: ").append(escapeHtml(String.join(", ", alert.areas())));
      }
      if (alert.publishedAt() != null) {
        builder.append("<br/>Published: ").append(escapeHtml(EMAIL_DATE_FORMATTER.format(alert.publishedAt())));
      }
      if (alert.url() != null && !alert.url().isBlank()) {
        builder.append("<br/><a href=\"").append(escapeHtml(alert.url())).append("\">Open source</a>");
      }
      builder.append("</li>");
    }

    builder.append("</ul></body></html>");
    return builder.toString();
  }

  private static String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
