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
import com.hashan0314.nordalert.backend.config.KrisinformationApiProperties;
import com.hashan0314.nordalert.backend.config.PublicApiProperties;
import com.hashan0314.nordalert.backend.models.KrisinformationItem;

@ExtendWith(MockitoExtension.class)
class KrisinformationAdapterTest {

  @Mock
  private HttpJsonClient httpJsonClient;

  private KrisinformationAdapter krisinformationAdapter;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PublicApiProperties properties = createProperties();

  @BeforeEach
  void setUp() {
    krisinformationAdapter = new KrisinformationAdapter(httpJsonClient, properties);
  }

  private static PublicApiProperties createProperties() {
    KrisinformationApiProperties krisinformation = new KrisinformationApiProperties();
    krisinformation.setAggregatedFeedUrl("https://api.krisinformation.se/v2/aggregatedfeed");

    PublicApiProperties properties = new PublicApiProperties();
    properties.setKrisinformation(krisinformation);
    return properties;
  }

  @Test
  void shouldNormalizeAggregatedFeedItems() throws Exception {
    when(httpJsonClient.getJson("https://api.krisinformation.se/v2/aggregatedfeed")).thenReturn(objectMapper.readTree("""
        [
          {
            "Identifier": "news-1",
            "Headline": "News headline",
            "Preamble": "News preamble",
            "BodyText": "Full crisis message",
            "Area": [{ "Description": "Stockholms län" }],
            "Published": "2026-03-12T16:52:18Z",
            "Web": "https://example.com/news-1",
            "PushMessage": "Push text"
          },
          {
            "id": "vma-1",
            "title": "VMA title",
            "counties": ["Skåne län"],
            "date": "2026-03-12T17:52:18Z",
            "url": "https://example.com/vma-1"
          }
        ]
        """));

    List<KrisinformationItem> items = krisinformationAdapter.fetchKrisinformationItems();

    assertEquals(2, items.size());
    assertEquals("news-1", items.get(0).id());
    assertEquals("Full crisis message", items.get(0).bodyText());
    assertEquals("VMA title", items.get(1).headline());
    assertEquals(Instant.parse("2026-03-12T17:52:18Z"), items.get(1).publishedAt());
    assertNull(items.get(1).pushMessage());
  }

  @Test
  void shouldStripHtmlFromKrisinformationText() throws Exception {
    when(httpJsonClient.getJson("https://api.krisinformation.se/v2/aggregatedfeed")).thenReturn(objectMapper.readTree("""
        [
          {
            "Identifier": "news-2",
            "Headline": "VMA heading",
            "Preamble": "<p>Ursprungligt meddelande:</p><p>Viktigt meddelande till allmänheten i Thoméegränd i Östersunds kommun.</p>",
            "BodyText": "<p>Ursprungligt meddelande:</p><p>Viktigt meddelande till allmänheten i Thoméegränd i Östersunds kommun. Det brinner just nu i ett garage.</p>\\n\\nUppdatering 18 mars 03.00: Meddelandet gäller inte längre. Faran är över.",
            "Area": [{ "Description": "Jämtlands län" }],
            "Published": "2026-03-18T02:00:00Z",
            "PushMessage": "Uppdatering 18 mars 03.00: Meddelandet gäller inte längre. Faran är över."
          }
        ]
        """));

    KrisinformationItem item = krisinformationAdapter.fetchKrisinformationItems().get(0);

    assertEquals(
        "Ursprungligt meddelande: Viktigt meddelande till allmänheten i Thoméegränd i Östersunds kommun. Det brinner just nu i ett garage. Uppdatering 18 mars 03.00: Meddelandet gäller inte längre. Faran är över.",
        item.bodyText()
    );
    assertEquals(
        "Ursprungligt meddelande: Viktigt meddelande till allmänheten i Thoméegränd i Östersunds kommun.",
        item.preamble()
    );
  }

  @Test
  void shouldNormalizeWhitespaceInKrisinformationText() throws Exception {
    when(httpJsonClient.getJson("https://api.krisinformation.se/v2/aggregatedfeed")).thenReturn(objectMapper.readTree("""
        [
          {
            "Identifier": "news-3",
            "Headline": "Whitespace test",
            "Preamble": "  Intro\\r\\n\\tNext line\\n\\n\\nThird\\u00A0line   ",
            "BodyText": "Alpha\\t\\tBeta \\n  Gamma\\r\\n\\n\\n\\nDelta\\u00A0\\u00A0Epsilon   \\n\\n",
            "Area": [{ "Description": "Västra Götalands län" }],
            "Published": "2026-03-20T10:00:00Z",
            "PushMessage": "\\tPush\\u00A0message \\r\\n  continued   "
          }
        ]
        """));

    KrisinformationItem item = krisinformationAdapter.fetchKrisinformationItems().get(0);

    assertEquals("Intro\nNext line\n\nThird line", item.preamble());
    assertEquals("Alpha Beta\nGamma\n\nDelta Epsilon", item.bodyText());
    assertEquals("Push message\ncontinued", item.pushMessage());
  }
}
