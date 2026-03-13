package com.hashan0314.nordalert.backend.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;

public record SmhiWarning(
    String id,
    String eventType,
    SmhiWarningLevel level,
    String description,
    List<String> areas,
    Instant validFrom,
    Instant validTo,
    String url,
    JsonNode geoJson
) {
}
