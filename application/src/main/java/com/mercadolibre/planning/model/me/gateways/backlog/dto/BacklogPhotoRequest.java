package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class BacklogPhotoRequest {
  private String logisticCenterId;

  private List<String> workflows;

  private List<String> processes;

  private List<String> steps;

  private Instant dateFrom;

  private Instant dateTo;

  private Instant dateInFrom;

  private Instant dateInTo;

  private Instant slaFrom;

  private Instant slaTo;

  private List<String> groupBy;
}
