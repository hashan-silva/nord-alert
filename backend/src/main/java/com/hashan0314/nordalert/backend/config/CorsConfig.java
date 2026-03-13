package com.hashan0314.nordalert.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns("http://localhost:3000", "https://*.cloudfront.net")
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
