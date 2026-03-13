package com.hashan0314.nordalert.backend.adapters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.SubscriptionProperties;
import com.hashan0314.nordalert.backend.models.AlertSource;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.Severity;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Component
public class DynamoDbSubscriptionAdapter {

  private static final String COUNTIES_ATTRIBUTE = "counties";
  private static final String CREATED_AT_ATTRIBUTE = "createdAt";
  private static final String EMAIL_ATTRIBUTE = "email";
  private static final String ID_ATTRIBUTE = "id";
  private static final String LAST_NOTIFIED_AT_ATTRIBUTE = "lastNotifiedAt";
  private static final String SEVERITY_ATTRIBUTE = "severity";
  private static final String SOURCES_ATTRIBUTE = "sources";

  private final DynamoDbClient dynamoDbClient;
  private final SubscriptionProperties subscriptionProperties;

  public DynamoDbSubscriptionAdapter(
      DynamoDbClient dynamoDbClient,
      SubscriptionProperties subscriptionProperties
  ) {
    this.dynamoDbClient = dynamoDbClient;
    this.subscriptionProperties = subscriptionProperties;
  }

  public AlertSubscription save(AlertSubscription subscription) {
    dynamoDbClient.putItem(PutItemRequest.builder()
        .tableName(subscriptionProperties.getTableName())
        .item(toItem(subscription))
        .build());
    return subscription;
  }

  public List<AlertSubscription> findAll() {
    return dynamoDbClient.scan(ScanRequest.builder()
            .tableName(subscriptionProperties.getTableName())
            .build())
        .items()
        .stream()
        .map(this::fromItem)
        .toList();
  }

  public void updateLastNotifiedAt(String subscriptionId, Instant lastNotifiedAt) {
    dynamoDbClient.updateItem(UpdateItemRequest.builder()
        .tableName(subscriptionProperties.getTableName())
        .key(Map.of(ID_ATTRIBUTE, stringAttribute(subscriptionId)))
        .updateExpression("SET #lastNotifiedAt = :lastNotifiedAt")
        .expressionAttributeNames(Map.of("#lastNotifiedAt", LAST_NOTIFIED_AT_ATTRIBUTE))
        .expressionAttributeValues(Map.of(
            ":lastNotifiedAt", stringAttribute(lastNotifiedAt.toString())
        ))
        .build());
  }

  private Map<String, AttributeValue> toItem(AlertSubscription subscription) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put(ID_ATTRIBUTE, stringAttribute(subscription.id()));
    item.put(EMAIL_ATTRIBUTE, stringAttribute(subscription.email()));
    item.put(CREATED_AT_ATTRIBUTE, stringAttribute(subscription.createdAt().toString()));
    item.put(COUNTIES_ATTRIBUTE, stringListAttribute(subscription.counties()));
    item.put(SOURCES_ATTRIBUTE, stringListAttribute(subscription.sources().stream()
        .map(AlertSource::value)
        .toList()));

    if (subscription.severity() != null) {
      item.put(SEVERITY_ATTRIBUTE, stringAttribute(subscription.severity().value()));
    }

    if (subscription.lastNotifiedAt() != null) {
      item.put(LAST_NOTIFIED_AT_ATTRIBUTE, stringAttribute(subscription.lastNotifiedAt().toString()));
    }

    return item;
  }

  private AlertSubscription fromItem(Map<String, AttributeValue> item) {
    return new AlertSubscription(
        item.get(ID_ATTRIBUTE).s(),
        item.get(EMAIL_ATTRIBUTE).s(),
        stringList(item.get(COUNTIES_ATTRIBUTE)),
        parseSeverity(item.get(SEVERITY_ATTRIBUTE)),
        stringList(item.get(SOURCES_ATTRIBUTE)).stream()
            .map(AlertSource::fromValue)
            .toList(),
        Instant.parse(item.get(CREATED_AT_ATTRIBUTE).s()),
        item.containsKey(LAST_NOTIFIED_AT_ATTRIBUTE)
            ? Instant.parse(item.get(LAST_NOTIFIED_AT_ATTRIBUTE).s())
            : null
    );
  }

  private static AttributeValue stringListAttribute(List<String> values) {
    List<AttributeValue> attributes = values.stream()
        .map(DynamoDbSubscriptionAdapter::stringAttribute)
        .toList();
    return AttributeValue.builder().l(attributes).build();
  }

  private static AttributeValue stringAttribute(String value) {
    return AttributeValue.builder().s(value).build();
  }

  private static List<String> stringList(AttributeValue value) {
    if (value == null || value.l() == null) {
      return List.of();
    }

    List<String> results = new ArrayList<>();
    for (AttributeValue entry : value.l()) {
      if (entry.s() != null && !entry.s().isBlank()) {
        results.add(entry.s());
      }
    }
    return results;
  }

  private static Severity parseSeverity(AttributeValue value) {
    if (value == null || value.s() == null || value.s().isBlank()) {
      return null;
    }
    return Severity.fromQuery(value.s());
  }
}
