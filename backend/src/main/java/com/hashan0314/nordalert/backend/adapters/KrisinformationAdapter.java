package com.hashan0314.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.KrisinformationItem;

@Component
public class KrisinformationAdapter {

  private static final String PUSH_MESSAGE_FIELD = "pushMessage";

  private final HttpJsonClient httpJsonClient;
  private final PublicApiProperties publicApiProperties;

  public KrisinformationAdapter(HttpJsonClient httpJsonClient, PublicApiProperties publicApiProperties) {
    this.httpJsonClient = httpJsonClient;
    this.publicApiProperties = publicApiProperties;
  }

  public List<KrisinformationItem> fetchKrisinformationItems() {
    JsonNode news = httpJsonClient.getJson(publicApiProperties.getKrisinformation().getNewsUrl());
    JsonNode vmas = httpJsonClient.getJson(publicApiProperties.getKrisinformation().getVmasUrl());

    List<KrisinformationItem> items = new ArrayList<>();
    items.addAll(normalize(news));
    items.addAll(normalize(vmas));
    return items;
  }

  private static List<KrisinformationItem> normalize(JsonNode items) {
    if (!items.isArray()) {
      return List.of();
    }

    List<KrisinformationItem> normalized = new ArrayList<>();
    for (JsonNode item : items) {
      normalized.add(new KrisinformationItem(
          item.path("id").asText(""),
          firstText(item.path("headline"), item.path("title")),
          item.path("preamble").asText(""),
          collectCounties(item.path("counties")),
          firstInstant(item.path("published").asText(null), item.path("date").asText(null)),
          firstText(item.path("web"), item.path("url")),
          item.path(PUSH_MESSAGE_FIELD).isMissingNode() || item.path(PUSH_MESSAGE_FIELD).isNull()
              ? null
              : item.path(PUSH_MESSAGE_FIELD).asText("")
      ));
    }
    return normalized;
  }

  private static List<String> collectCounties(JsonNode countiesNode) {
    if (!countiesNode.isArray()) {
      return List.of();
    }

    List<String> counties = new ArrayList<>();
    for (JsonNode county : countiesNode) {
      if (!county.isNull()) {
        String value = county.asText("");
        if (!value.isBlank()) {
          counties.add(value);
        }
      }
    }
    return counties;
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
}
