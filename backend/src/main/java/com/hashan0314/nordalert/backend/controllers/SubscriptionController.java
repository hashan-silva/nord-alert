package com.hashan0314.nordalert.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.hashan0314.nordalert.backend.models.AlertSubscription;
import com.hashan0314.nordalert.backend.models.CreateAlertSubscriptionRequest;
import com.hashan0314.nordalert.backend.services.SubscriptionService;

@RestController
@Tag(name = "Subscriptions", description = "Email subscriptions for alert delivery")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping("/subscriptions")
  @Operation(
      summary = "List subscriptions",
      responses = @ApiResponse(
          responseCode = "200",
          description = "Current subscriptions",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertSubscription.class)))
      )
  )
  public List<AlertSubscription> subscriptions() {
    return subscriptionService.getSubscriptions();
  }

  @PostMapping("/subscriptions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create email subscription",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Subscription created",
            content = @Content(schema = @Schema(implementation = AlertSubscription.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid subscription request")
      }
  )
  public AlertSubscription createSubscription(@Valid @RequestBody CreateAlertSubscriptionRequest request) {
    try {
      return subscriptionService.createSubscription(request);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  @GetMapping(value = "/subscriptions/confirm", produces = MediaType.TEXT_HTML_VALUE)
  @Operation(
      summary = "Confirm email subscription",
      responses = {
        @ApiResponse(responseCode = "200", description = "Subscription confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid confirmation token")
      }
  )
  public ResponseEntity<String> confirmSubscription(@RequestParam String token) {
    try {
      AlertSubscription subscription = subscriptionService.confirmSubscription(token);
      String html = """
          <html><body style="font-family:Arial,sans-serif;color:#15324b;padding:32px;">
            <h2>Subscription confirmed</h2>
            <p>Email alerts are now active for <strong>%s</strong>.</p>
          </body></html>
          """.formatted(subscription.email());
      return ResponseEntity.ok(html);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }
}
