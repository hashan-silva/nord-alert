package com.hashan0314.nordalert.backend;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.hashan0314.nordalert.backend.models.SubscriptionDispatchResult;
import com.hashan0314.nordalert.backend.services.SubscriptionDispatchService;

public class SubscriptionDispatchLambdaHandler implements RequestHandler<Map<String, Object>, SubscriptionDispatchResult> {

  private static final ConfigurableApplicationContext APPLICATION_CONTEXT = new SpringApplicationBuilder(NordAlertApplication.class)
      .web(WebApplicationType.NONE)
      .run();

  @Override
  public SubscriptionDispatchResult handleRequest(Map<String, Object> input, Context context) {
    return APPLICATION_CONTEXT.getBean(SubscriptionDispatchService.class).dispatchPendingAlerts();
  }
}
