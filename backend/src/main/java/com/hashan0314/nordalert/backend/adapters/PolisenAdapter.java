package com.hashan0314.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.PolisenEvent;
import com.hashan0314.nordalert.backend.models.PolisenLocation;

@Component
public class PolisenAdapter {

  private final HttpJsonClient httpJsonClient;
  private final PublicApiProperties publicApiProperties;

  public PolisenAdapter(HttpJsonClient httpJsonClient, PublicApiProperties publicApiProperties) {
    this.httpJsonClient = httpJsonClient;
    this.publicApiProperties = publicApiProperties;
  }

  public List<PolisenEvent> fetchPolisenEvents() {
    JsonNode events = httpJsonClient.getJson(publicApiProperties.getPolisen().getEventsUrl());
    if (!events.isArray()) {
      return List.of();
    }

    List<PolisenEvent> results = new ArrayList<>();
    for (JsonNode event : events) {
      String eventId = text(event.path("id"), text(event.path("eventid")));
      String[] coordinates = parseGps(text(event.path("location").path("gps"))).split(",");
      Double latitude = parseCoordinate(coordinates, 0);
      Double longitude = parseCoordinate(coordinates, 1);

      results.add(new PolisenEvent(
          eventId,
          text(event.path("name")),
          text(event.path("type")),
          text(event.path("summary")),
          normalizeUrl(text(event.path("url")), eventId, publicApiProperties.getPolisen().getBaseUrl()),
          DateParser.parse(text(event.path("datetime"))),
          new PolisenLocation(
              text(event.path("location").path("name")),
              latitude,
              longitude
          )
      ));
    }
    return results;
  }

  private static String text(JsonNode node) {
    return text(node, "");
  }

  private static String text(JsonNode node, String fallback) {
    return node.isMissingNode() || node.isNull() ? fallback : node.asText(fallback);
  }

  private static String normalizeUrl(String url, String id) {
    if (url == null || url.isBlank()) {
      return "/aktuellt/handelser/" + id;
    }

    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }

    return url;
  }

  private static String parseGps(String gps) {
    return gps == null ? "" : gps.trim();
  }

  private static Double parseCoordinate(String[] coordinates, int index) {
    if (coordinates.length <= index) {
      return null;
    }

    try {
      return Double.parseDouble(coordinates[index].trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String normalizeUrl(String url, String id, String baseUrl) {
    String normalized = normalizeUrl(url, id);
    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return normalized;
    }
    return normalized.startsWith("/") ? baseUrl + normalized : baseUrl + "/" + normalized;
  }
}
