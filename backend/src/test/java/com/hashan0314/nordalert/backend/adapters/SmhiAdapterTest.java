package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.config.SmhiApiProperties;
import com.hashan0314.nordalert.backend.models.SmhiWarning;
import com.hashan0314.nordalert.backend.models.SmhiWarningLevel;

@ExtendWith(MockitoExtension.class)
class SmhiAdapterTest {

  @Mock
  private HttpJsonClient httpJsonClient;

  private SmhiAdapter smhiAdapter;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PublicApiProperties properties = createProperties();

  @BeforeEach
  void setUp() {
    smhiAdapter = new SmhiAdapter(httpJsonClient, properties);
  }

  private static PublicApiProperties createProperties() {
    SmhiApiProperties smhi = new SmhiApiProperties();
    smhi.setWarningsUrl("https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json");

    PublicApiProperties properties = new PublicApiProperties();
    properties.setSmhi(smhi);
    return properties;
  }

  @Test
  void shouldNormalizeSmhiWarnings() throws Exception {
    when(httpJsonClient.getJson("https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json"))
        .thenReturn(objectMapper.readTree("""
            [
              {
                "id": "3049",
                "created": "2026-03-12T00:00:00Z",
                "event": {
                  "en": "Low sea level"
                },
                "warningAreas": [
                  {
                    "id": "9668",
                    "warningLevel": {
                      "code": "red"
                    },
                    "descriptions": [
                      {
                        "text": {
                          "en": "Warning text"
                        }
                      }
                    ],
                    "affectedAreas": [
                      {
                        "en": "The Belts"
                      }
                    ],
                    "area": {
                      "type": "Feature",
                      "geometry": {
                        "type": "Polygon",
                        "coordinates": [[[12.0, 55.0], [13.0, 55.0], [13.0, 56.0], [12.0, 55.0]]]
                      }
                    },
                    "approximateStart": "2026-03-13T09:00:00Z",
                    "approximateEnd": "2026-03-13T12:00:00Z"
                  }
                ]
              }
            ]
            """));

    List<SmhiWarning> warnings = smhiAdapter.fetchSmhiWarnings();

    assertEquals(1, warnings.size());
    assertEquals("3049-9668", warnings.get(0).id());
    assertEquals(SmhiWarningLevel.RED, warnings.get(0).level());
    assertEquals(List.of("Skåne län"), warnings.get(0).areas());
    assertEquals(Instant.parse("2026-03-13T09:00:00Z"), warnings.get(0).validFrom());
    assertNotNull(warnings.get(0).geoJson());
  }

  @Test
  void shouldPreserveCountyAreasWhenAlreadyMappedToCounties() throws Exception {
    when(httpJsonClient.getJson("https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json"))
        .thenReturn(objectMapper.readTree("""
            [
              {
                "id": "3051",
                "created": "2026-03-12T00:00:00Z",
                "event": {
                  "sv": "Snöfall"
                },
                "warningAreas": [
                  {
                    "id": "9673",
                    "warningLevel": {
                      "code": "orange"
                    },
                    "descriptions": [],
                    "affectedAreas": [
                      {
                        "sv": "Stockholms län"
                      }
                    ],
                    "published": "2026-03-13T08:00:00Z"
                  }
                ]
              }
            ]
            """));

    SmhiWarning warning = smhiAdapter.fetchSmhiWarnings().get(0);

    assertEquals(List.of("Stockholms län"), warning.areas());
    assertEquals("", warning.url());
  }

  @Test
  void shouldMapMessageLevelToYellow() throws Exception {
    when(httpJsonClient.getJson("https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json"))
        .thenReturn(objectMapper.readTree("""
            [
              {
                "id": "3050",
                "created": "2026-03-12T00:00:00Z",
                "event": {
                  "sv": "Fire risk"
                },
                "warningAreas": [
                  {
                    "id": "9672",
                    "warningLevel": {
                      "code": "message"
                    },
                    "descriptions": [],
                    "affectedAreas": [],
                    "published": "2026-03-13T08:00:00Z"
                  }
                ]
              }
            ]
            """));

    SmhiWarning warning = smhiAdapter.fetchSmhiWarnings().get(0);

    assertEquals(SmhiWarningLevel.YELLOW, warning.level());
    assertEquals(Instant.parse("2026-03-13T08:00:00Z"), warning.validFrom());
    assertNull(warning.geoJson());
  }
}
