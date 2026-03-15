package com.hashan0314.nordalert.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.adapters.DynamoDbSubscriptionAdapter;
import com.hashan0314.nordalert.backend.adapters.SesEmailAdapter;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.CreateAlertSubscriptionRequest;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @Mock
  private DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter;

  @Mock
  private SesEmailAdapter sesEmailAdapter;

  @Test
  void shouldNormalizeAndSaveSubscriptionRequest() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );
    when(dynamoDbSubscriptionAdapter.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CreateAlertSubscriptionRequest request = new CreateAlertSubscriptionRequest();
    request.setEmail(" ops@example.com ");
    request.setCounties(List.of("Stockholms län", " ", "Stockholms län", "Skåne län"));
    request.setSeverity(" medium ");
    request.setSources(List.of("polisen", "smhi", "polisen"));

    AlertSubscription saved = subscriptionService.createSubscription(request);

    assertNotNull(saved.id());
    assertEquals("ops@example.com", saved.email());
    assertEquals(List.of("Stockholms län", "Skåne län"), saved.counties());
    assertEquals(Severity.MEDIUM, saved.severity());
    assertEquals(List.of(AlertSource.POLISEN, AlertSource.SMHI), saved.sources());
    assertEquals(SubscriptionStatus.PENDING, saved.status());
    assertNotNull(saved.confirmationToken());
    assertNotNull(saved.createdAt());
    assertNull(saved.confirmedAt());
    assertNull(saved.lastNotifiedAt());
    verify(sesEmailAdapter).ensureEmailIdentity("ops@example.com");
  }

  @Test
  void shouldAllowBlankSeverityAndMissingFilters() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );
    when(dynamoDbSubscriptionAdapter.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CreateAlertSubscriptionRequest request = new CreateAlertSubscriptionRequest();
    request.setEmail("ops@example.com");
    request.setSeverity(" ");
    request.setCounties(null);
    request.setSources(null);

    AlertSubscription saved = subscriptionService.createSubscription(request);

    assertNull(saved.severity());
    assertEquals(List.of(), saved.counties());
    assertEquals(List.of(), saved.sources());
  }

  @Test
  void shouldRejectUnknownSources() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );

    CreateAlertSubscriptionRequest request = new CreateAlertSubscriptionRequest();
    request.setEmail("ops@example.com");
    request.setSources(List.of("unknown"));

    assertThrows(IllegalArgumentException.class, () -> subscriptionService.createSubscription(request));
  }

  @Test
  void shouldForwardMarkNotifiedToAdapter() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );
    Instant notifiedAt = Instant.parse("2026-03-13T11:00:00Z");

    subscriptionService.markNotified("sub-1", notifiedAt);

    verify(dynamoDbSubscriptionAdapter).updateLastNotifiedAt("sub-1", notifiedAt);
  }

  @Test
  void shouldReturnSubscriptionsFromAdapter() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );
    when(dynamoDbSubscriptionAdapter.findAll()).thenReturn(List.of(
        new AlertSubscription(
            "sub-1",
            "ops@example.com",
            List.of(),
            null,
            List.of(),
            SubscriptionStatus.CONFIRMED,
            null,
            Instant.parse("2026-03-13T10:00:00Z"),
            Instant.parse("2026-03-13T10:05:00Z"),
            Instant.parse("2026-03-13T10:00:00Z")
        )
    ));

    List<AlertSubscription> subscriptions = subscriptionService.getSubscriptions();

    assertEquals(1, subscriptions.size());
    assertEquals("sub-1", subscriptions.get(0).id());
  }

  @Test
  void shouldConfirmPendingSubscription() {
    SubscriptionService subscriptionService = new SubscriptionService(
        dynamoDbSubscriptionAdapter,
        sesEmailAdapter
    );
    when(dynamoDbSubscriptionAdapter.findByConfirmationToken("token-1")).thenReturn(
        new AlertSubscription(
            "sub-1",
            "ops@example.com",
            List.of("Stockholms län"),
            Severity.LOW,
            List.of(AlertSource.POLISEN),
            SubscriptionStatus.PENDING,
            "token-1",
            Instant.parse("2026-03-13T10:00:00Z"),
            null,
            null
        )
    );

    AlertSubscription confirmed = subscriptionService.confirmSubscription("token-1");

    assertEquals(SubscriptionStatus.CONFIRMED, confirmed.status());
    assertNull(confirmed.confirmationToken());
    assertNotNull(confirmed.confirmedAt());
    assertNotNull(confirmed.lastNotifiedAt());
    verify(dynamoDbSubscriptionAdapter).confirmSubscription(eq("sub-1"), any());
  }
}
