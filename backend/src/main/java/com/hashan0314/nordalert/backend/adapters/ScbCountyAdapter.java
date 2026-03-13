package com.hashan0314.nordalert.backend.adapters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import com.hashan0314.nordalert.backend.models.County;

@Component
public class ScbCountyAdapter {

  static final String SCB_COUNTIES_URL =
      "https://www.scb.se/en/finding-statistics/regional-statistics/regional-divisions/"
          + "counties-and-municipalities/counties-and-municipalities-in-numerical-order/";

  private final HttpTextClient httpTextClient;

  public ScbCountyAdapter(HttpTextClient httpTextClient) {
    this.httpTextClient = httpTextClient;
  }

  public List<County> fetchCounties() {
    Document document = Jsoup.parse(httpTextClient.getText(SCB_COUNTIES_URL));
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
