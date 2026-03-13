package com.hashan0314.nordalert.backend.adapters;

import java.util.List;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.SubscriptionProperties;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
public class SesEmailAdapter {

  private final SesV2Client sesV2Client;
  private final SubscriptionProperties subscriptionProperties;

  public SesEmailAdapter(SesV2Client sesV2Client, SubscriptionProperties subscriptionProperties) {
    this.sesV2Client = sesV2Client;
    this.subscriptionProperties = subscriptionProperties;
  }

  public void sendEmail(String recipient, String subject, String textBody, String htmlBody) {
    sesV2Client.sendEmail(SendEmailRequest.builder()
        .fromEmailAddress(subscriptionProperties.getSenderEmail())
        .destination(Destination.builder().toAddresses(List.of(recipient)).build())
        .content(EmailContent.builder()
            .simple(Message.builder()
                .subject(Content.builder().data(subject).build())
                .body(Body.builder()
                    .text(Content.builder().data(textBody).build())
                    .html(Content.builder().data(htmlBody).build())
                    .build())
                .build())
            .build())
        .build());
  }
}
