package com.hashan0314.nordalert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nordalert.api")
public class PublicApiProperties {

  private PolisenApiProperties polisen = new PolisenApiProperties();
  private SmhiApiProperties smhi = new SmhiApiProperties();
  private KrisinformationApiProperties krisinformation = new KrisinformationApiProperties();
  private ScbApiProperties scb = new ScbApiProperties();

  public PolisenApiProperties getPolisen() {
    return polisen;
  }

  public void setPolisen(PolisenApiProperties polisen) {
    this.polisen = polisen;
  }

  public SmhiApiProperties getSmhi() {
    return smhi;
  }

  public void setSmhi(SmhiApiProperties smhi) {
    this.smhi = smhi;
  }

  public KrisinformationApiProperties getKrisinformation() {
    return krisinformation;
  }

  public void setKrisinformation(KrisinformationApiProperties krisinformation) {
    this.krisinformation = krisinformation;
  }

  public ScbApiProperties getScb() {
    return scb;
  }

  public void setScb(ScbApiProperties scb) {
    this.scb = scb;
  }
}
