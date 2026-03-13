package com.hashan0314.nordalert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nordalert.subscription")
public class SubscriptionProperties {

  private String tableName;
  private String senderEmail;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getSenderEmail() {
    return senderEmail;
  }

  public void setSenderEmail(String senderEmail) {
    this.senderEmail = senderEmail;
  }
}
