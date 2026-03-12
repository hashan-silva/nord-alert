package com.hashan0314.nordalert.backend.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeverityTest {

  @Test
  void shouldResolveSeverityIgnoringCase() {
    assertEquals(Severity.MEDIUM, Severity.fromQuery("MeDiUm"));
  }

  @Test
  void shouldThrowForUnsupportedSeverity() {
    assertThrows(IllegalArgumentException.class, () -> Severity.fromQuery("critical"));
  }
}
