package se.nordalert.backend.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Alert(
    AlertSource source,
    String id,
    String headline,
    String description,
    List<String> areas,
    Severity severity,
    Instant publishedAt,
    String url
) {
}
