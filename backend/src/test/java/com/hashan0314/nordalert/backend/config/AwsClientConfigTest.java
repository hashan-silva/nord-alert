package com.hashan0314.nordalert.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AwsClientConfigTest {

  @Test
  void shouldCreateClientsWithConfiguredRegion() {
    AwsClientConfig config = new AwsClientConfig("eu-north-1");

    assertEquals("eu-north-1", config.dynamoDbClient().serviceClientConfiguration().region().id());
    assertEquals("eu-north-1", config.sesV2Client().serviceClientConfiguration().region().id());
  }
}
