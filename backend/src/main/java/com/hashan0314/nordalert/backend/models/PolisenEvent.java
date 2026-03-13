package com.hashan0314.nordalert.backend.models;

import java.time.Instant;

public record PolisenEvent(
    String id,
    String title,
    String type,
    String summary,
    String url,
    Instant occurredAt,
    PolisenLocation location
) {
}
