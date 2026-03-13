package com.hashan0314.nordalert.backend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(PublicApiProperties.class)
public class NordAlertApplication {

  public static void main(String[] args) {
    SpringApplication.run(NordAlertApplication.class, args);
  }
}
