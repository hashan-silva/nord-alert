package com.hashan0314.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SmhiAdapter {

  private static final String SMHI_WARNINGS_URL =
      "https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json";
  private static final String EVENT_FIELD = "event";

  private final HttpJsonClient httpJsonClient;

  public SmhiAdapter(HttpJsonClient httpJsonClient) {
    this.httpJsonClient = httpJsonClient;
  }

  public List<SmhiWarning> fetchSmhiWarnings() {
    JsonNode data = httpJsonClient.getJson(SMHI_WARNINGS_URL);
    if (!data.isArray()) {
      return List.of();
    }

    List<SmhiWarning> warnings = new ArrayList<>();
    for (JsonNode warning : data) {
      String eventType = firstText(
          warning.path(EVENT_FIELD).path("en"),
          warning.path(EVENT_FIELD).path("sv"),
          warning.path(EVENT_FIELD).path("code")
      );

      for (JsonNode area : warning.path("warningAreas")) {
        String levelCode = area.path("warningLevel").path("code").asText("").toLowerCase();
        WarningLevel level = mapLevel(levelCode);
        String description = joinDescriptions(area.path("descriptions"));
        List<String> areas = collectStrings(area.path("affectedAreas"));
        Instant validFrom = firstInstant(
            area.path("approximateStart").asText(null),
            area.path("published").asText(null),
            warning.path("created").asText(null)
        );
        Instant validTo = firstInstant(
            area.path("approximateEnd").asText(null),
            area.path("approximateStart").asText(null),
            area.path("published").asText(null),
            warning.path("created").asText(null)
        );

        warnings.add(new SmhiWarning(
            warning.path("id").asText("") + "-" + area.path("id").asText(""),
            eventType,
            level,
            description,
            areas,
            validFrom,
            validTo,
            ""
        ));
      }
    }

    return warnings;
  }

  private static WarningLevel mapLevel(String levelCode) {
    return switch (levelCode) {
      case "orange" -> WarningLevel.ORANGE;
      case "red" -> WarningLevel.RED;
      default -> WarningLevel.YELLOW;
    };
  }

  private static String joinDescriptions(JsonNode descriptions) {
    List<String> values = new ArrayList<>();
    for (JsonNode description : descriptions) {
      String text = firstText(description.path("text").path("en"), description.path("text").path("sv"));
      if (!text.isBlank()) {
        values.add(text);
      }
    }
    return String.join("\n", values);
  }

  private static List<String> collectStrings(JsonNode affectedAreas) {
    List<String> values = new ArrayList<>();
    for (JsonNode area : affectedAreas) {
      String text = firstText(area.path("en"), area.path("sv"));
      if (!text.isBlank()) {
        values.add(text);
      }
    }
    return values;
  }

  private static String firstText(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (!node.isMissingNode() && !node.isNull()) {
        String value = node.asText("");
        if (!value.isBlank()) {
          return value;
        }
      }
    }
    return "";
  }

  private static Instant firstInstant(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return DateParser.parse(value);
      }
    }
    return Instant.EPOCH;
  }

  public record SmhiWarning(
      String id,
      String eventType,
      WarningLevel level,
      String description,
      List<String> areas,
      Instant validFrom,
      Instant validTo,
      String url
  ) {
  }

  public enum WarningLevel {
    YELLOW,
    ORANGE,
    RED
  }
}
