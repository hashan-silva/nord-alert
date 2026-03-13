package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.config.SubscriptionProperties;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.Severity;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@ExtendWith(MockitoExtension.class)
class DynamoDbSubscriptionAdapterTest {

  @Mock
  private DynamoDbClient dynamoDbClient;

  @Test
  void shouldPersistSubscriptionToConfiguredTable() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setTableName("subscriptions");
    DynamoDbSubscriptionAdapter adapter = new DynamoDbSubscriptionAdapter(dynamoDbClient, properties);

    AlertSubscription subscription = new AlertSubscription(
        "sub-1",
        "ops@example.com",
        List.of("Stockholms län"),
        Severity.HIGH,
        List.of(AlertSource.POLISEN, AlertSource.SMHI),
        Instant.parse("2026-03-13T10:00:00Z"),
        Instant.parse("2026-03-13T11:00:00Z")
    );

    adapter.save(subscription);

    ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(dynamoDbClient).putItem(captor.capture());

    PutItemRequest request = captor.getValue();
    assertEquals("subscriptions", request.tableName());
    assertEquals("sub-1", request.item().get("id").s());
    assertEquals("ops@example.com", request.item().get("email").s());
    assertEquals("high", request.item().get("severity").s());
    assertEquals(List.of("polisen", "smhi"),
        request.item().get("sources").l().stream().map(AttributeValue::s).toList());
  }

  @Test
  void shouldReadSubscriptionsFromScanResponse() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setTableName("subscriptions");
    DynamoDbSubscriptionAdapter adapter = new DynamoDbSubscriptionAdapter(dynamoDbClient, properties);

    when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(ScanResponse.builder()
        .items(Map.of(
            "id", AttributeValue.builder().s("sub-1").build(),
            "email", AttributeValue.builder().s("ops@example.com").build(),
            "counties", AttributeValue.builder().l(
                AttributeValue.builder().s("Stockholms län").build()
            ).build(),
            "severity", AttributeValue.builder().s("medium").build(),
            "sources", AttributeValue.builder().l(
                AttributeValue.builder().s("polisen").build()
            ).build(),
            "createdAt", AttributeValue.builder().s("2026-03-13T10:00:00Z").build()
        ))
        .build());

    List<AlertSubscription> subscriptions = adapter.findAll();

    assertEquals(1, subscriptions.size());
    assertEquals("sub-1", subscriptions.get(0).id());
    assertEquals(List.of("Stockholms län"), subscriptions.get(0).counties());
    assertEquals(Severity.MEDIUM, subscriptions.get(0).severity());
    assertEquals(List.of(AlertSource.POLISEN), subscriptions.get(0).sources());
    assertNull(subscriptions.get(0).lastNotifiedAt());
  }

  @Test
  void shouldUpdateLastNotifiedAt() {
    SubscriptionProperties properties = new SubscriptionProperties();
    properties.setTableName("subscriptions");
    DynamoDbSubscriptionAdapter adapter = new DynamoDbSubscriptionAdapter(dynamoDbClient, properties);

    adapter.updateLastNotifiedAt("sub-1", Instant.parse("2026-03-13T11:00:00Z"));

    ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
    verify(dynamoDbClient).updateItem(captor.capture());

    UpdateItemRequest request = captor.getValue();
    assertEquals("subscriptions", request.tableName());
    assertEquals("sub-1", request.key().get("id").s());
    assertEquals("2026-03-13T11:00:00Z",
        request.expressionAttributeValues().get(":lastNotifiedAt").s());
  }
}
