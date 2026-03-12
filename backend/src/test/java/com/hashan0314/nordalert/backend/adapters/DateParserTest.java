package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DateParserTest {

  @Test
  void shouldParseIsoInstant() {
    Instant parsed = DateParser.parse("2026-03-12T16:52:18Z");

    assertEquals(Instant.parse("2026-03-12T16:52:18Z"), parsed);
  }

  @Test
  void shouldParsePolisenTimestampWithSingleDigitHour() {
    Instant parsed = DateParser.parse("2026-03-05 7:56:56 +01:00");

    assertEquals(Instant.parse("2026-03-05T06:56:56Z"), parsed);
  }

  @Test
  void shouldReturnEpochForInvalidValues() {
    Instant parsed = DateParser.parse("not-a-date");

    assertEquals(Instant.EPOCH, parsed);
  }
}
