package com.hashan0314.nordalert.backend.adapters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.County;

@Component
public class ScbCountyAdapter {

  private final HttpTextClient httpTextClient;
  private final PublicApiProperties publicApiProperties;

  public ScbCountyAdapter(HttpTextClient httpTextClient, PublicApiProperties publicApiProperties) {
    this.httpTextClient = httpTextClient;
    this.publicApiProperties = publicApiProperties;
  }

  public List<County> fetchCounties() {
    Document document = Jsoup.parse(httpTextClient.getText(publicApiProperties.getScb().getCountiesUrl()));
    Map<String, County> counties = new LinkedHashMap<>();

    for (Element heading : document.select("h2")) {
      County county = parseCounty(heading.text());
      if (county != null) {
        counties.put(county.code(), county);
      }
    }

    return counties.values().stream().toList();
  }

  private static County parseCounty(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim().replaceAll("\\s+", " ");
    if (!normalized.matches("\\d{2} .+ län")) {
      return null;
    }

    int separatorIndex = normalized.indexOf(' ');
    String code = normalized.substring(0, separatorIndex);
    String name = normalized.substring(separatorIndex + 1);
    return new County(code, name);
  }
}
