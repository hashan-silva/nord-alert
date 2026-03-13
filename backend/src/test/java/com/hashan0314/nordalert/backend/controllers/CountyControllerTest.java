package com.hashan0314.nordalert.backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.hashan0314.nordalert.backend.models.County;
import com.hashan0314.nordalert.backend.services.CountyService;

@WebMvcTest(CountyController.class)
class CountyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CountyService countyService;

  @Test
  void shouldReturnCounties() throws Exception {
    when(countyService.fetchCounties()).thenReturn(List.of(
        new County("01", "Stockholms län"),
        new County("12", "Skåne län")
    ));

    mockMvc.perform(get("/counties"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].code").value("01"))
        .andExpect(jsonPath("$[0].name").value("Stockholms län"));
  }
}
