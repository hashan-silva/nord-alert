package com.hashan0314.nordalert.backend.models;

import io.swagger.v3.oas.annotations.media.Schema;

public record HealthResponse(
    @Schema(example = "ok")
    String status
) {
}
