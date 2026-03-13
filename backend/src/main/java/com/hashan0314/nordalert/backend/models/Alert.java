package com.hashan0314.nordalert.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Alert(
    @Schema(example = "polisen")
    AlertSource source,
    @Schema(example = "627250")
    String id,
    @Schema(example = "12 mars 17.32, Brand, Goteborg")
    String headline,
    @Schema(example = "Vi far larm om en brand i ett radhus.")
    String description,
    @Schema(example = "[\"Vastra Gotalands lan\"]")
    List<String> areas,
    @Schema(example = "info")
    Severity severity,
    @Schema(example = "2026-03-12T16:52:18Z")
    Instant publishedAt,
    @Schema(example = "https://polisen.se/aktuellt/handelser/2026/mars/12/12-mars-17.32-brand-goteborg/")
    String url,
    @Schema(example = "58.252793")
    Double latitude,
    @Schema(example = "13.059643")
    Double longitude,
    JsonNode geoJson
) {
}
