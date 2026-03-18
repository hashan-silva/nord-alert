package com.hashan0314.nordalert.backend.models;

import java.time.Instant;
import java.util.List;

public record KrisinformationItem(
    String id,
    String headline,
    String preamble,
    String bodyText,
    List<String> counties,
    Instant publishedAt,
    String url,
    String pushMessage
) {
}
