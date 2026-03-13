package com.hashan0314.nordalert.backend.models;

import java.time.Instant;
import java.util.List;

public record AlertSubscription(
    String id,
    String email,
    List<String> counties,
    Severity severity,
    List<AlertSource> sources,
    Instant createdAt,
    Instant lastNotifiedAt
) {
}
