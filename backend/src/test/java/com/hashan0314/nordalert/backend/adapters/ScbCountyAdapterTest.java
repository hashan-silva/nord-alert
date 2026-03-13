package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hashan0314.nordalert.backend.models.County;

@ExtendWith(MockitoExtension.class)
class ScbCountyAdapterTest {

  @Mock
  private HttpTextClient httpTextClient;

  private ScbCountyAdapter scbCountyAdapter;

  @BeforeEach
  void setUp() {
    scbCountyAdapter = new ScbCountyAdapter(httpTextClient);
  }

  @Test
  void shouldExtractCountiesFromScbHeadings() {
    when(httpTextClient.getText(ScbCountyAdapter.SCB_COUNTIES_URL)).thenReturn("""
        <html>
          <body>
            <h1>Counties and municipalities in numerical order</h1>
            <h2>01 Stockholms län</h2>
            <p>0114 Upplands Väsby</p>
            <h2>03 Uppsala län</h2>
            <h2>12 Skåne län</h2>
          </body>
        </html>
        """);

    List<County> counties = scbCountyAdapter.fetchCounties();

    assertEquals(List.of(
        new County("01", "Stockholms län"),
        new County("03", "Uppsala län"),
        new County("12", "Skåne län")
    ), counties);
  }
}
