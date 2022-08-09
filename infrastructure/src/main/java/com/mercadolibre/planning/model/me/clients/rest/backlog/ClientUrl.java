package com.mercadolibre.planning.model.me.clients.rest.backlog;

//TODO: Delete filters (not url) when backlog client deprecated method is not used
public enum ClientUrl {
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

  ClientUrl(final String name) {
    this.name = name;
  }

  ClientUrl() {

  }

  public String getName() {
    return name != null ? name.toLowerCase() : name().toLowerCase();
  }

}
