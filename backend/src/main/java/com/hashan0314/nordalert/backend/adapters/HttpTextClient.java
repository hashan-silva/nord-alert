package com.hashan0314.nordalert.backend.adapters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HttpTextClient {

  private final HttpClient httpClient;

  public HttpTextClient() {
    this.httpClient = HttpClient.newHttpClient();
  }

  public String getText(String url) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Request failed with status " + response.statusCode() + " for " + url);
      }
      return response.body();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read text from " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Request interrupted for " + url, e);
    }
  }
}
