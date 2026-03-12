package com.hashan0314.nordalert.backend.adapters;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateParser {

  private static final DateTimeFormatter SPACE_OFFSET_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XX");
  private static final DateTimeFormatter SPACE_OFFSET_LONG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
  private static final DateTimeFormatter SPACE_NO_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter SPACE_OFFSET_SHORT_SINGLE_HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss XX");
  private static final DateTimeFormatter SPACE_OFFSET_LONG_SINGLE_HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss XXX");
  private static final DateTimeFormatter SPACE_NO_OFFSET_SINGLE_HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");

  private DateParser() {
  }

  public static Instant parse(String value) {
    if (value == null || value.isBlank()) {
      return Instant.EPOCH;
    }

    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return OffsetDateTime.parse(value, SPACE_OFFSET_SHORT).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return OffsetDateTime.parse(value, SPACE_OFFSET_LONG).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return OffsetDateTime.parse(value, SPACE_OFFSET_SHORT_SINGLE_HOUR).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return OffsetDateTime.parse(value, SPACE_OFFSET_LONG_SINGLE_HOUR).toInstant();
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return LocalDateTime.parse(value, SPACE_NO_OFFSET).toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException ignored) {
      // Try the next known format.
    }

    try {
      return LocalDateTime.parse(value, SPACE_NO_OFFSET_SINGLE_HOUR).toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException ignored) {
      // Return the fallback below.
    }

    return Instant.EPOCH;
  }
}
