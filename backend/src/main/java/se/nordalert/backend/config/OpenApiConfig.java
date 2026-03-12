package se.nordalert.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI nordAlertOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("NordAlert Backend API")
            .version("1.0.0")
            .description("Aggregated public alert feed for NordAlert clients."));
  }
}
