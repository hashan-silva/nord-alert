package com.hashan0314.nordalert.backend.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hashan0314.nordalert.backend.models.County;
import com.hashan0314.nordalert.backend.services.CountyService;

@RestController
@Tag(name = "Counties", description = "County reference data")
public class CountyController {

  private final CountyService countyService;

  public CountyController(CountyService countyService) {
    this.countyService = countyService;
  }

  @GetMapping("/counties")
  @Operation(
      summary = "List counties",
      description = "Returns the Sweden county list sourced from Statistics Sweden (SCB).",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "County reference data",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = County.class)))
        )
      }
  )
  public List<County> counties() {
    return countyService.fetchCounties();
  }
}
