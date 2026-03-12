package se.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HttpJsonClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public HttpJsonClient(ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.objectMapper = objectMapper;
  }

  public JsonNode getJson(String url) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Request failed with status " + response.statusCode() + " for " + url);
      }
      return objectMapper.readTree(response.body());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse JSON from " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Request interrupted for " + url, e);
    }
  }
}
