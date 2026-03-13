package com.hashan0314.nordalert.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.hashan0314.nordalert.backend.adapters.DynamoDbSubscriptionAdapter;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.CreateAlertSubscriptionRequest;
import com.hashan0314.nordalert.backend.models.Severity;

@Service
public class SubscriptionService {

  private final DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter;

  public SubscriptionService(DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter) {
    this.dynamoDbSubscriptionAdapter = dynamoDbSubscriptionAdapter;
  }

  public AlertSubscription createSubscription(CreateAlertSubscriptionRequest request) {
    Instant now = Instant.now();
    AlertSubscription subscription = new AlertSubscription(
        UUID.randomUUID().toString(),
        request.getEmail().trim(),
        normalizeValues(request.getCounties()),
        parseSeverity(request.getSeverity()),
        normalizeValues(request.getSources()).stream()
            .map(AlertSource::fromValue)
            .toList(),
        now,
        now
    );
    return dynamoDbSubscriptionAdapter.save(subscription);
  }

  public List<AlertSubscription> getSubscriptions() {
    return dynamoDbSubscriptionAdapter.findAll();
  }

  public void markNotified(String subscriptionId, Instant lastNotifiedAt) {
    dynamoDbSubscriptionAdapter.updateLastNotifiedAt(subscriptionId, lastNotifiedAt);
  }

  private static List<String> normalizeValues(List<String> values) {
    if (values == null) {
      return List.of();
    }

    return values.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .distinct()
        .toList();
  }

  private static Severity parseSeverity(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Severity.fromQuery(value.trim());
  }
}
