package com.hashan0314.nordalert.backend.models;

public record SubscriptionDispatchResult(
    int processedSubscriptions,
    int emailedSubscriptions
) {
}
