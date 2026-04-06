package com.hashan0314.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.KrisinformationItem;

@Component
public class KrisinformationAdapter {

  private static final String PUSH_MESSAGE_FIELD = "pushMessage";
  private static final String PUSH_MESSAGE_PASCAL_CASE_FIELD = "PushMessage";

  private final HttpJsonClient httpJsonClient;
  private final PublicApiProperties publicApiProperties;

  public KrisinformationAdapter(HttpJsonClient httpJsonClient, PublicApiProperties publicApiProperties) {
    this.httpJsonClient = httpJsonClient;
    this.publicApiProperties = publicApiProperties;
  }

  public List<KrisinformationItem> fetchKrisinformationItems() {
    JsonNode aggregatedFeed = httpJsonClient.getJson(publicApiProperties.getKrisinformation().getAggregatedFeedUrl());
    return normalize(aggregatedFeed);
  }

  private static List<KrisinformationItem> normalize(JsonNode items) {
    if (!items.isArray()) {
      return List.of();
    }

    List<KrisinformationItem> normalized = new ArrayList<>();
    for (JsonNode item : items) {
      normalized.add(new KrisinformationItem(
          firstText(item.path("id"), item.path("Identifier")),
          normalizedText(item.path("headline"), item.path("Headline"), item.path("title"), item.path("Title")),
          normalizedText(item.path("preamble"), item.path("Preamble")),
          normalizedText(item.path("bodyText"), item.path("BodyText")),
          collectAreas(item),
          firstInstant(
              item.path("published").asText(null),
              item.path("Published").asText(null),
              item.path("date").asText(null),
              item.path("Updated").asText(null)
          ),
          normalizedText(item.path("web"), item.path("Web"), item.path("url"), item.path("Url")),
          isMissingOrNull(item.path(PUSH_MESSAGE_FIELD), item.path(PUSH_MESSAGE_PASCAL_CASE_FIELD))
              ? null
              : normalizedText(item.path(PUSH_MESSAGE_FIELD), item.path(PUSH_MESSAGE_PASCAL_CASE_FIELD))
      ));
    }
    return normalized;
  }

  private static List<String> collectAreas(JsonNode item) {
    JsonNode countiesNode = item.path("counties");
    if (!countiesNode.isArray()) {
      JsonNode areaNode = item.path("Area");
      if (!areaNode.isArray()) {
        return List.of();
      }

      List<String> areas = new ArrayList<>();
      for (JsonNode area : areaNode) {
        String description = firstText(area.path("Description"), area.path("description"));
        if (!description.isBlank()) {
          areas.add(description);
        }
      }
      return areas;
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

  private static String normalizedText(JsonNode... nodes) {
    return normalizeHtmlText(firstText(nodes));
  }

  private static String normalizeHtmlText(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }

    String text = extractTextContent(value).replace('\u00A0', ' ');
    StringBuilder normalized = new StringBuilder(text.length());
    int consecutiveNewlines = 0;
    boolean pendingSpace = false;

    for (int index = 0; index < text.length(); index++) {
      char current = text.charAt(index);

      if (shouldSkip(current)) {
        // Ignore carriage returns and keep processing the remaining content.
      } else if (current == '\n') {
        trimTrailingSpace(normalized);
        consecutiveNewlines = appendNormalizedNewline(normalized, consecutiveNewlines);
        pendingSpace = false;
      } else if (isInlineWhitespace(current)) {
        pendingSpace = shouldQueueSpace(normalized, consecutiveNewlines);
      } else {
        if (pendingSpace) {
          normalized.append(' ');
        }

        normalized.append(current);
        consecutiveNewlines = 0;
        pendingSpace = false;
      }
    }

    return trimTrailingWhitespace(normalized);
  }

  private static String extractTextContent(String value) {
    if (looksLikeHtml(value)) {
      return Jsoup.parse(value).text();
    }
    return value;
  }

  private static boolean looksLikeHtml(String value) {
    return value.indexOf('<') >= 0 && value.indexOf('>') >= 0;
  }

  private static boolean shouldSkip(char current) {
    return current == '\r';
  }

  private static boolean isInlineWhitespace(char current) {
    return current == ' ' || current == '\t';
  }

  private static boolean shouldQueueSpace(StringBuilder normalized, int consecutiveNewlines) {
    return normalized.length() > 0 && consecutiveNewlines == 0;
  }

  private static void trimTrailingSpace(StringBuilder normalized) {
    int length = normalized.length();
    if (length > 0 && normalized.charAt(length - 1) == ' ') {
      normalized.setLength(length - 1);
    }
  }

  private static int appendNormalizedNewline(StringBuilder normalized, int consecutiveNewlines) {
    if (consecutiveNewlines < 2) {
      normalized.append('\n');
    }
    return consecutiveNewlines + 1;
  }

  private static String trimTrailingWhitespace(StringBuilder normalized) {
    int end = normalized.length();
    while (end > 0 && isTrimmedWhitespace(normalized.charAt(end - 1))) {
      end--;
    }
    return normalized.substring(0, end);
  }

  private static boolean isTrimmedWhitespace(char current) {
    return current == ' ' || current == '\n' || current == '\t';
  }

  private static boolean isMissingOrNull(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (!node.isMissingNode() && !node.isNull()) {
        return false;
      }
    }
    return true;
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
