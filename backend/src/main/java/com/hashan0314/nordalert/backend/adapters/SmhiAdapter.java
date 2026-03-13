package com.hashan0314.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SmhiAdapter {

  private static final String SMHI_WARNINGS_URL =
      "https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json";
  private static final String EVENT_FIELD = "event";
  private static final Map<String, List<String>> SEA_DISTRICT_COUNTY_MAP = Map.ofEntries(
      Map.entry("Skagerrak", List.of("Västra Götalands län")),
      Map.entry("Kattegat", List.of("Skåne län", "Hallands län", "Västra Götalands län")),
      Map.entry("Kattegatt", List.of("Skåne län", "Hallands län", "Västra Götalands län")),
      Map.entry("The Sound", List.of("Skåne län")),
      Map.entry("Öresund", List.of("Skåne län")),
      Map.entry("The Belts", List.of("Skåne län")),
      Map.entry("Bälten", List.of("Skåne län")),
      Map.entry("Western Baltic", List.of("Skåne län")),
      Map.entry("Sydvästra Östersjön", List.of("Skåne län")),
      Map.entry("Southern Baltic", List.of("Skåne län", "Blekinge län", "Kalmar län")),
      Map.entry("Södra Östersjön", List.of("Skåne län", "Blekinge län", "Kalmar län")),
      Map.entry("South-eastern Baltic", List.of("Kalmar län", "Gotlands län")),
      Map.entry("Sydöstra Östersjön", List.of("Kalmar län", "Gotlands län")),
      Map.entry("Central Baltic", List.of("Östergötlands län", "Kalmar län", "Gotlands län", "Stockholms län")),
      Map.entry("Mellersta Östersjön", List.of("Östergötlands län", "Kalmar län", "Gotlands län", "Stockholms län")),
      Map.entry("Northern Baltic", List.of("Stockholms län", "Uppsala län", "Södermanlands län")),
      Map.entry("Norra Östersjön", List.of("Stockholms län", "Uppsala län", "Södermanlands län")),
      Map.entry("Sea of Åland and Archipelago Sea", List.of("Stockholms län", "Uppsala län")),
      Map.entry("Sea of Aaland and Archipelago Sea", List.of("Stockholms län", "Uppsala län")),
      Map.entry("Ålands hav och Skärgårdshavet", List.of("Stockholms län", "Uppsala län")),
      Map.entry("Sea of Bothnia", List.of("Gävleborgs län", "Västernorrlands län")),
      Map.entry("Southern Sea of Bothnia", List.of("Gävleborgs län", "Västernorrlands län")),
      Map.entry("Bottenhavet", List.of("Gävleborgs län", "Västernorrlands län")),
      Map.entry("Northern Sea of Bothnia", List.of("Västernorrlands län", "Västerbottens län")),
      Map.entry("Norra Bottenhavet", List.of("Västernorrlands län", "Västerbottens län")),
      Map.entry("The Quark", List.of("Västernorrlands län", "Västerbottens län")),
      Map.entry("Norra Kvarken", List.of("Västernorrlands län", "Västerbottens län")),
      Map.entry("Bay of Bothnia", List.of("Västerbottens län", "Norrbottens län")),
      Map.entry("Bottenviken", List.of("Västerbottens län", "Norrbottens län")),
      Map.entry("Lake Vänern and Trollhätte Canal", List.of("Västra Götalands län", "Värmlands län")),
      Map.entry("Vänern och Trollhätte kanal", List.of("Västra Götalands län", "Värmlands län")),
      Map.entry("Lake Mälaren and Södertälje Canal", List.of("Stockholms län", "Södermanlands län", "Uppsala län", "Västmanlands län")),
      Map.entry("Mälaren och Södertälje kanal", List.of("Stockholms län", "Södermanlands län", "Uppsala län", "Västmanlands län"))
  );

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
          warning.path(EVENT_FIELD).path("sv"),
          warning.path(EVENT_FIELD).path("en"),
          warning.path(EVENT_FIELD).path("code")
      );

      for (JsonNode area : warning.path("warningAreas")) {
        String levelCode = area.path("warningLevel").path("code").asText("").toLowerCase();
        WarningLevel level = mapLevel(levelCode);
        String description = joinDescriptions(area.path("descriptions"));
        List<String> areas = collectAreas(area.path("affectedAreas"));
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
            "",
            geoJson(area.path("area"))
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
      String text = firstText(description.path("text").path("sv"), description.path("text").path("en"));
      if (!text.isBlank()) {
        values.add(text);
      }
    }
    return String.join("\n", values);
  }

  private static List<String> collectAreas(JsonNode affectedAreas) {
    Set<String> values = new LinkedHashSet<>();
    for (JsonNode area : affectedAreas) {
      String text = firstText(area.path("sv"), area.path("en"));
      if (!text.isBlank()) {
        values.addAll(mapToCounties(text));
      }
    }
    return new ArrayList<>(values);
  }

  private static List<String> mapToCounties(String area) {
    return SEA_DISTRICT_COUNTY_MAP.getOrDefault(area, List.of(area));
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

  private static JsonNode geoJson(JsonNode value) {
    return value.isMissingNode() || value.isNull() ? null : value;
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
      String url,
      JsonNode geoJson
  ) {
  }

  public enum WarningLevel {
    YELLOW,
    ORANGE,
    RED
  }
}
