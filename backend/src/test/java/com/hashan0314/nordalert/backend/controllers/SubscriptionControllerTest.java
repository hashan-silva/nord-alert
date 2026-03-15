package com.hashan0314.nordalert.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.Severity;
import com.hashan0314.nordalert.backend.models.SubscriptionStatus;
import com.hashan0314.nordalert.backend.services.SubscriptionService;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SubscriptionService subscriptionService;

  @Test
  void shouldCreateSubscription() throws Exception {
    when(subscriptionService.createSubscription(any())).thenReturn(new AlertSubscription(
        "sub-1",
        "ops@example.com",
        List.of("Stockholms län"),
        Severity.MEDIUM,
        List.of(AlertSource.POLISEN),
        SubscriptionStatus.PENDING,
        "token-1",
        Instant.parse("2026-03-13T10:00:00Z"),
        null,
        null
    ));

    mockMvc.perform(post("/subscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "ops@example.com",
                  "counties": ["Stockholms län"],
                  "severity": "medium",
                  "sources": ["polisen"]
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ops@example.com"))
        .andExpect(jsonPath("$.sources[0]").value("polisen"))
        .andExpect(jsonPath("$.status").value("pending"));
  }

  @Test
  void shouldRejectInvalidSubscriptionEmail() throws Exception {
    mockMvc.perform(post("/subscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "not-an-email"
                }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldListSubscriptions() throws Exception {
    when(subscriptionService.getSubscriptions()).thenReturn(List.of(
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

    mockMvc.perform(get("/subscriptions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("sub-1"));
  }

  @Test
  void shouldConfirmSubscription() throws Exception {
    when(subscriptionService.confirmSubscription("token-1")).thenReturn(new AlertSubscription(
        "sub-1",
        "ops@example.com",
        List.of(),
        null,
        List.of(),
        SubscriptionStatus.CONFIRMED,
        null,
        Instant.parse("2026-03-13T10:00:00Z"),
        Instant.parse("2026-03-13T10:05:00Z"),
        Instant.parse("2026-03-13T10:05:00Z")
    ));

    mockMvc.perform(get("/subscriptions/confirm").param("token", "token-1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Subscription confirmed")));
  }
}
