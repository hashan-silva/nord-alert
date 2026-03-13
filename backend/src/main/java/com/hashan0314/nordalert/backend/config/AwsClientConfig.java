package com.hashan0314.nordalert.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class AwsClientConfig {

  private final Region region;

  public AwsClientConfig(@Value("${aws.region}") String region) {
    this.region = Region.of(region);
  }

  @Bean
  public DynamoDbClient dynamoDbClient() {
    return DynamoDbClient.builder()
        .region(region)
        .build();
  }

  @Bean
  public SesV2Client sesV2Client() {
    return SesV2Client.builder()
        .region(region)
        .build();
  }
}
