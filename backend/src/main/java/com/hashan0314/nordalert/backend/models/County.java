package com.hashan0314.nordalert.backend.models;

import io.swagger.v3.oas.annotations.media.Schema;

public record County(
    @Schema(example = "01")
    String code,
    @Schema(example = "Stockholms län")
    String name
) {
}
