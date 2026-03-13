package com.hashan0314.nordalert.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.adapters.DynamoDbSubscriptionAdapter;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.CreateAlertSubscriptionRequest;
import com.hashan0314.nordalert.backend.models.Severity;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @Mock
  private DynamoDbSubscriptionAdapter dynamoDbSubscriptionAdapter;

  @Test
  void shouldNormalizeAndSaveSubscriptionRequest() {
    SubscriptionService subscriptionService = new SubscriptionService(dynamoDbSubscriptionAdapter);
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
    assertNotNull(saved.createdAt());
    assertNotNull(saved.lastNotifiedAt());
  }

  @Test
  void shouldAllowBlankSeverityAndMissingFilters() {
    SubscriptionService subscriptionService = new SubscriptionService(dynamoDbSubscriptionAdapter);
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
    SubscriptionService subscriptionService = new SubscriptionService(dynamoDbSubscriptionAdapter);

    CreateAlertSubscriptionRequest request = new CreateAlertSubscriptionRequest();
    request.setEmail("ops@example.com");
    request.setSources(List.of("unknown"));

    assertThrows(IllegalArgumentException.class, () -> subscriptionService.createSubscription(request));
  }

  @Test
  void shouldForwardMarkNotifiedToAdapter() {
    SubscriptionService subscriptionService = new SubscriptionService(dynamoDbSubscriptionAdapter);
    Instant notifiedAt = Instant.parse("2026-03-13T11:00:00Z");

    subscriptionService.markNotified("sub-1", notifiedAt);

    verify(dynamoDbSubscriptionAdapter).updateLastNotifiedAt("sub-1", notifiedAt);
  }

  @Test
  void shouldReturnSubscriptionsFromAdapter() {
    SubscriptionService subscriptionService = new SubscriptionService(dynamoDbSubscriptionAdapter);
    when(dynamoDbSubscriptionAdapter.findAll()).thenReturn(List.of(
        new AlertSubscription(
            "sub-1",
            "ops@example.com",
            List.of(),
            null,
            List.of(),
            Instant.parse("2026-03-13T10:00:00Z"),
            Instant.parse("2026-03-13T10:00:00Z")
        )
    ));

    List<AlertSubscription> subscriptions = subscriptionService.getSubscriptions();

    assertEquals(1, subscriptions.size());
    assertEquals("sub-1", subscriptions.get(0).id());
  }
}
