package com.hashan0314.nordalert.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void shouldConfigureOpenApiMetadata() {
    OpenApiConfig config = new OpenApiConfig();

    OpenAPI openAPI = config.nordAlertOpenApi();

    assertEquals("NordAlert Backend API", openAPI.getInfo().getTitle());
    assertEquals("2.0.0", openAPI.getInfo().getVersion());
  }
}
