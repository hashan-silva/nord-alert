package se.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PolisenAdapter {

  private static final String POLISEN_EVENTS_URL = "https://polisen.se/api/events";

  private final HttpJsonClient httpJsonClient;

  public PolisenAdapter(HttpJsonClient httpJsonClient) {
    this.httpJsonClient = httpJsonClient;
  }

  public List<PolisenEvent> fetchPolisenEvents() {
    JsonNode events = httpJsonClient.getJson(POLISEN_EVENTS_URL);
    if (!events.isArray()) {
      return List.of();
    }

    List<PolisenEvent> results = new ArrayList<>();
    for (JsonNode event : events) {
      String[] coordinates = parseGps(text(event.path("location").path("gps"))).split(",");
      Double latitude = parseCoordinate(coordinates, 0);
      Double longitude = parseCoordinate(coordinates, 1);

      results.add(new PolisenEvent(
          text(event.path("id"), text(event.path("eventid"))),
          text(event.path("name")),
          text(event.path("type")),
          text(event.path("summary")),
          text(event.path("url"), "https://polisen.se/aktuellt/handelser/" + text(event.path("id"))),
          DateParser.parse(text(event.path("datetime"))),
          new Location(
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

  public record PolisenEvent(
      String id,
      String title,
      String type,
      String summary,
      String url,
      Instant occurredAt,
      Location location
  ) {
  }

  public record Location(
      String name,
      Double lat,
      Double lon
  ) {
  }
}
