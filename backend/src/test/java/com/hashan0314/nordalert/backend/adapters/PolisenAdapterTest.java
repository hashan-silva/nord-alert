package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.config.PolisenApiProperties;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.PolisenEvent;

@ExtendWith(MockitoExtension.class)
class PolisenAdapterTest {

  @Mock
  private HttpJsonClient httpJsonClient;

  private PolisenAdapter polisenAdapter;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PublicApiProperties properties = createProperties();

  @BeforeEach
  void setUp() {
    polisenAdapter = new PolisenAdapter(httpJsonClient, properties);
  }

  private static PublicApiProperties createProperties() {
    PolisenApiProperties polisen = new PolisenApiProperties();
    polisen.setBaseUrl("https://polisen.se");
    polisen.setEventsUrl("https://polisen.se/api/events");

    PublicApiProperties properties = new PublicApiProperties();
    properties.setPolisen(polisen);
    return properties;
  }

  @Test
  void shouldNormalizePolisenEvents() throws Exception {
    when(httpJsonClient.getJson("https://polisen.se/api/events")).thenReturn(objectMapper.readTree("""
        [
          {
            "id": 627250,
            "datetime": "2026-03-12 17:52:18 +01:00",
            "name": "Brand, Goteborg",
            "summary": "Vi far larm om en brand i ett radhus.",
            "url": "/aktuellt/handelser/2026/mars/12/brand-goteborg/",
            "type": "Brand",
            "location": {
              "name": "Vastra Gotalands lan",
              "gps": "58.252793,13.059643"
            }
          }
        ]
        """));

    List<PolisenEvent> events = polisenAdapter.fetchPolisenEvents();

    assertEquals(1, events.size());
    assertEquals("627250", events.get(0).id());
    assertEquals(Instant.parse("2026-03-12T16:52:18Z"), events.get(0).occurredAt());
    assertEquals(58.252793, events.get(0).location().lat());
    assertEquals("https://polisen.se/aktuellt/handelser/2026/mars/12/brand-goteborg/", events.get(0).url());
  }

  @Test
  void shouldHandleMissingGpsCoordinates() throws Exception {
    when(httpJsonClient.getJson("https://polisen.se/api/events")).thenReturn(objectMapper.readTree("""
        [
          {
            "eventid": "legacy-id",
            "datetime": "2026-03-12T16:52:18Z",
            "name": "Info",
            "location": {
              "name": "Stockholms lan"
            }
          }
        ]
        """));

    PolisenEvent event = polisenAdapter.fetchPolisenEvents().get(0);

    assertEquals("legacy-id", event.id());
    assertNull(event.location().lat());
    assertNull(event.location().lon());
    assertEquals("https://polisen.se/aktuellt/handelser/legacy-id", event.url());
  }

  @Test
  void shouldKeepAbsolutePolisenUrl() throws Exception {
    when(httpJsonClient.getJson("https://polisen.se/api/events")).thenReturn(objectMapper.readTree("""
        [
          {
            "id": "627250",
            "datetime": "2026-03-12T16:52:18Z",
            "url": "https://polisen.se/aktuellt/handelser/2026/mars/12/brand-goteborg/",
            "location": {
              "name": "Stockholms lan"
            }
          }
        ]
        """));

    PolisenEvent event = polisenAdapter.fetchPolisenEvents().get(0);

    assertEquals("https://polisen.se/aktuellt/handelser/2026/mars/12/brand-goteborg/", event.url());
  }
}
