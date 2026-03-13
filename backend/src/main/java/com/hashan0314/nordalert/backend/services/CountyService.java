package com.hashan0314.nordalert.backend.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.hashan0314.nordalert.backend.adapters.ScbCountyAdapter;
import com.hashan0314.nordalert.backend.models.County;

@Service
public class CountyService {

  private final ScbCountyAdapter scbCountyAdapter;

  public CountyService(ScbCountyAdapter scbCountyAdapter) {
    this.scbCountyAdapter = scbCountyAdapter;
  }

  public List<County> fetchCounties() {
    return scbCountyAdapter.fetchCounties();
  }
}
