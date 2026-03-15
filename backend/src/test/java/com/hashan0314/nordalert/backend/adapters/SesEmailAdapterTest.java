package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.config.SubscriptionProperties;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.CreateEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesEmailAdapterTest {

  @Mock
  private SesV2Client sesV2Client;

  @Test
  void shouldBuildExpectedSendEmailRequest() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setSenderEmail("noreply@nordalert.se");

    SesEmailAdapter adapter = new SesEmailAdapter(sesV2Client, properties);
    adapter.sendEmail("ops@example.com", "NordAlert test", "Plain body", "<p>Html body</p>");

    ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
    verify(sesV2Client).sendEmail(captor.capture());

    SendEmailRequest request = captor.getValue();
    assertEquals("noreply@nordalert.se", request.fromEmailAddress());
    assertEquals("ops@example.com", request.destination().toAddresses().get(0));
    assertEquals("NordAlert test", request.content().simple().subject().data());
    assertEquals("Plain body", request.content().simple().body().text().data());
    assertEquals("<p>Html body</p>", request.content().simple().body().html().data());
  }

  @Test
  void shouldCreateEmailIdentityForRecipient() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setSenderEmail("noreply@nordalert.se");

    SesEmailAdapter adapter = new SesEmailAdapter(sesV2Client, properties);
    adapter.ensureEmailIdentity("ops@example.com");

    ArgumentCaptor<CreateEmailIdentityRequest> captor = ArgumentCaptor.forClass(CreateEmailIdentityRequest.class);
    verify(sesV2Client).createEmailIdentity(captor.capture());
    assertEquals("ops@example.com", captor.getValue().emailIdentity());
  }

  @Test
  void shouldReportVerifiedEmailIdentity() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setSenderEmail("noreply@nordalert.se");

    when(sesV2Client.getEmailIdentity(any(GetEmailIdentityRequest.class))).thenReturn(
        GetEmailIdentityResponse.builder()
            .verificationStatus("SUCCESS")
            .build()
    );

    SesEmailAdapter adapter = new SesEmailAdapter(sesV2Client, properties);

    assertTrue(adapter.isEmailIdentityVerified("ops@example.com"));
  }
}
