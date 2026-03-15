package com.hashan0314.nordalert.backend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.hashan0314.nordalert.backend.adapters.DynamoDbSubscriptionAdapter;
import com.hashan0314.nordalert.backend.adapters.SesEmailAdapter;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.CreateAlertSubscriptionRequest;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SubscriptionStatus;

@Service
public class SubscriptionService {

  private final DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter;
  private final SesEmailAdapter sesEmailAdapter;

  public SubscriptionService(
      DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter,
      SesEmailAdapter sesEmailAdapter
  ) {
    this.dynamoDbSubscriptionAdapter = dynamoDbSubscriptionAdapter;
    this.sesEmailAdapter = sesEmailAdapter;
  }

  public AlertSubscription createSubscription(CreateAlertSubscriptionRequest request) {
    Instant now = Instant.now();
    String confirmationToken = UUID.randomUUID().toString();
    AlertSubscription subscription = new AlertSubscription(
        UUID.randomUUID().toString(),
        request.getEmail().trim(),
        normalizeValues(request.getCounties()),
        parseSeverity(request.getSeverity()),
        normalizeValues(request.getSources()).stream()
            .map(AlertSource::fromValue)
            .toList(),
        SubscriptionStatus.PENDING,
        confirmationToken,
        now,
        null,
        null
    );
    AlertSubscription savedSubscription = dynamoDbSubscriptionAdapter.save(subscription);

    sesEmailAdapter.ensureEmailIdentity(savedSubscription.email());

    return savedSubscription;
  }

  public List<AlertSubscription> getSubscriptions() {
    return syncConfirmedSubscriptions(dynamoDbSubscriptionAdapter.findAll());
  }

  public void markNotified(String subscriptionId, Instant lastNotifiedAt) {
    dynamoDbSubscriptionAdapter.updateLastNotifiedAt(subscriptionId, lastNotifiedAt);
  }

  public AlertSubscription confirmSubscription(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("Invalid confirmation token");
    }

    AlertSubscription subscription = dynamoDbSubscriptionAdapter.findByConfirmationToken(token);
    if (subscription == null) {
      throw new IllegalArgumentException("Invalid confirmation token");
    }

    if (subscription.status() == SubscriptionStatus.CONFIRMED) {
      return subscription;
    }

    Instant confirmedAt = Instant.now();
    dynamoDbSubscriptionAdapter.confirmSubscription(subscription.id(), confirmedAt);
    return new AlertSubscription(
        subscription.id(),
        subscription.email(),
        subscription.counties(),
        subscription.severity(),
        subscription.sources(),
        SubscriptionStatus.CONFIRMED,
        null,
        subscription.createdAt(),
        confirmedAt,
        confirmedAt
    );
  }

  public List<AlertSubscription> syncConfirmedSubscriptions(List<AlertSubscription> subscriptions) {
    List<AlertSubscription> resolvedSubscriptions = new ArrayList<>();

    for (AlertSubscription subscription : subscriptions) {
      if (subscription.status() == SubscriptionStatus.PENDING
          && sesEmailAdapter.isEmailIdentityVerified(subscription.email())) {
        Instant confirmedAt = Instant.now();
        dynamoDbSubscriptionAdapter.confirmSubscription(subscription.id(), confirmedAt);
        resolvedSubscriptions.add(new AlertSubscription(
            subscription.id(),
            subscription.email(),
            subscription.counties(),
            subscription.severity(),
            subscription.sources(),
            SubscriptionStatus.CONFIRMED,
            null,
            subscription.createdAt(),
            confirmedAt,
            confirmedAt
        ));
        continue;
      }

      resolvedSubscriptions.add(subscription);
    }

    return resolvedSubscriptions;
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
