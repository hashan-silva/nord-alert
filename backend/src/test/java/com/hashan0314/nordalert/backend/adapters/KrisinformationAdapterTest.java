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

@ExtendWith(MockitoExtension.class)
class KrisinformationAdapterTest {

  @Mock
  private HttpJsonClient httpJsonClient;

  private KrisinformationAdapter krisinformationAdapter;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    krisinformationAdapter = new KrisinformationAdapter(httpJsonClient);
  }

  @Test
  void shouldMergeNewsAndVmasItems() throws Exception {
    when(httpJsonClient.getJson("https://api.krisinformation.se/v3/news")).thenReturn(objectMapper.readTree("""
        [
          {
            "id": "news-1",
            "headline": "News headline",
            "preamble": "News preamble",
            "counties": ["Stockholms län"],
            "published": "2026-03-12T16:52:18Z",
            "web": "https://example.com/news-1",
            "pushMessage": "Push text"
          }
        ]
        """));
    when(httpJsonClient.getJson("https://api.krisinformation.se/v3/vmas")).thenReturn(objectMapper.readTree("""
        [
          {
            "id": "vma-1",
            "title": "VMA title",
            "counties": ["Skåne län"],
            "date": "2026-03-12T17:52:18Z",
            "url": "https://example.com/vma-1"
          }
        ]
        """));

    List<KrisinformationAdapter.KrisinformationItem> items = krisinformationAdapter.fetchKrisinformationItems();

    assertEquals(2, items.size());
    assertEquals("news-1", items.get(0).id());
    assertEquals("VMA title", items.get(1).headline());
    assertEquals(Instant.parse("2026-03-12T17:52:18Z"), items.get(1).publishedAt());
    assertNull(items.get(1).pushMessage());
  }
}
