package com.hashan0314.nordalert.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.adapters.SesEmailAdapter;
import com.hashan0314.nordalert.backend.models.Alert;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SubscriptionDispatchResult;
import com.hashan0314.nordalert.backend.models.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionDispatchServiceTest {

  @Mock
  private AlertAggregationService alertAggregationService;

  @Mock
  private SubscriptionService subscriptionService;

  @Mock
  private SesEmailAdapter sesEmailAdapter;

  @Test
  void shouldSendEmailsForMatchingNewAlerts() {
    SubscriptionDispatchService service = new SubscriptionDispatchService(
        alertAggregationService,
        subscriptionService,
        sesEmailAdapter
    );

    when(subscriptionService.getSubscriptions()).thenReturn(List.of(
        new AlertSubscription(
            "sub-1",
        "ops@example.com",
        List.of("Stockholms län"),
        Severity.MEDIUM,
        List.of(AlertSource.POLISEN),
        SubscriptionStatus.CONFIRMED,
        null,
        Instant.parse("2026-03-13T09:00:00Z"),
        Instant.parse("2026-03-13T09:30:00Z"),
        Instant.parse("2026-03-13T09:30:00Z")
        )
    ));
    when(alertAggregationService.fetchAllAlerts()).thenReturn(List.of(
        new Alert(
            AlertSource.POLISEN,
            "p-1",
            "Police headline",
            "Description",
            List.of("Stockholms län"),
            Severity.HIGH,
            Instant.parse("2026-03-13T10:00:00Z"),
            "https://example.com/p-1",
            null,
            null,
            null
        ),
        new Alert(
            AlertSource.SMHI,
            "s-1",
            "Weather headline",
            "Description",
            List.of("Stockholms län"),
            Severity.HIGH,
            Instant.parse("2026-03-13T10:05:00Z"),
            "https://example.com/s-1",
            null,
            null,
            null
        )
    ));

    SubscriptionDispatchResult result = service.dispatchPendingAlerts();

    assertEquals(1, result.processedSubscriptions());
    assertEquals(1, result.emailedSubscriptions());
    verify(sesEmailAdapter).sendEmail(eq("ops@example.com"), anyString(), anyString(), anyString());
    verify(subscriptionService).markNotified("sub-1", Instant.parse("2026-03-13T10:00:00Z"));
  }

  @Test
  void shouldSkipEmailsWhenNoAlertMatchesSubscription() {
    SubscriptionDispatchService service = new SubscriptionDispatchService(
        alertAggregationService,
        subscriptionService,
        sesEmailAdapter
    );

    when(subscriptionService.getSubscriptions()).thenReturn(List.of(
        new AlertSubscription(
            "sub-1",
        "ops@example.com",
        List.of("Skåne län"),
        Severity.HIGH,
        List.of(AlertSource.POLISEN),
        SubscriptionStatus.CONFIRMED,
        null,
        Instant.parse("2026-03-13T09:00:00Z"),
        Instant.parse("2026-03-13T09:30:00Z"),
        Instant.parse("2026-03-13T09:30:00Z")
        )
    ));
    when(alertAggregationService.fetchAllAlerts()).thenReturn(List.of(
        new Alert(
            AlertSource.POLISEN,
            "p-1",
            "Police headline",
            "Description",
            List.of("Stockholms län"),
            Severity.MEDIUM,
            Instant.parse("2026-03-13T10:00:00Z"),
            "https://example.com/p-1",
            null,
            null,
            null
        )
    ));

    SubscriptionDispatchResult result = service.dispatchPendingAlerts();

    assertEquals(1, result.processedSubscriptions());
    assertEquals(0, result.emailedSubscriptions());
    verify(sesEmailAdapter, never()).sendEmail(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void shouldSkipPendingSubscriptions() {
    SubscriptionDispatchService service = new SubscriptionDispatchService(
        alertAggregationService,
        subscriptionService,
        sesEmailAdapter
    );

    when(subscriptionService.getSubscriptions()).thenReturn(List.of(
        new AlertSubscription(
            "sub-1",
            "ops@example.com",
            List.of("Stockholms län"),
            Severity.LOW,
            List.of(AlertSource.POLISEN),
            SubscriptionStatus.PENDING,
            "token-1",
            Instant.parse("2026-03-13T09:00:00Z"),
            null,
            null
        )
    ));
    when(alertAggregationService.fetchAllAlerts()).thenReturn(List.of(
        new Alert(
            AlertSource.POLISEN,
            "p-1",
            "Police headline",
            "Description",
            List.of("Stockholms län"),
            Severity.HIGH,
            Instant.parse("2026-03-13T10:00:00Z"),
            "https://example.com/p-1",
            null,
            null,
            null
        )
    ));

    SubscriptionDispatchResult result = service.dispatchPendingAlerts();

    assertEquals(1, result.processedSubscriptions());
    assertEquals(0, result.emailedSubscriptions());
    verify(sesEmailAdapter, never()).sendEmail(anyString(), anyString(), anyString(), anyString());
  }
}
