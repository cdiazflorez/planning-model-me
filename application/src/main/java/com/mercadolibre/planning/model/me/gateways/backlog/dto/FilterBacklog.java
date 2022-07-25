package com.mercadolibre.planning.model.me.gateways.backlog.dto;

public enum FilterBacklog {
  BACKLOG_URL("/backlogs/logistic_centers/%s/backlogs"),
  CURRENT_BACKLOG_URL("/backlogs/logistic_centers/%s/backlogs/current"),
  PHOTO_URL("/backlogs/logistic_centers/%s/photos"),
  LAST_PHOTO_URL("/backlogs/logistic_centers/%s/photos/last"),
  WORKFLOWS,
  STEPS,
  REQUEST_DATE("requestDate"),
  PROCESSES,
  GROUP_BY,
  DATE_FROM,
  DATE_TO,
  DATE_IN_FROM,
  DATE_IN_TO,
  SLA_FROM,
  SLA_TO,
  LOGISTIC_CENTER_ID,
  DATE_OUT_FROM,
  DATE_OUT_TO,
  PHOTO_DATE_TO("photoDateTo");

  public String name;

  FilterBacklog(final String name) {
    this.name = name;
  }

  FilterBacklog() {

  }

  public String getName() {
    return name().toLowerCase();
  }

}
