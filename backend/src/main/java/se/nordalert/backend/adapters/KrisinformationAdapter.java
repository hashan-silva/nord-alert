package se.nordalert.backend.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KrisinformationAdapter {

  private static final String NEWS_ENDPOINT = "https://api.krisinformation.se/v3/news";
  private static final String VMAS_ENDPOINT = "https://api.krisinformation.se/v3/vmas";

  private final HttpJsonClient httpJsonClient;

  public KrisinformationAdapter(HttpJsonClient httpJsonClient) {
    this.httpJsonClient = httpJsonClient;
  }

  public List<KrisinformationItem> fetchKrisinformationItems() {
    JsonNode news = httpJsonClient.getJson(NEWS_ENDPOINT);
    JsonNode vmas = httpJsonClient.getJson(VMAS_ENDPOINT);

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
          item.path("pushMessage").isMissingNode() || item.path("pushMessage").isNull()
              ? null
              : item.path("pushMessage").asText("")
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

  public record KrisinformationItem(
      String id,
      String headline,
      String preamble,
      List<String> counties,
      Instant publishedAt,
      String url,
      String pushMessage
  ) {
  }
}
