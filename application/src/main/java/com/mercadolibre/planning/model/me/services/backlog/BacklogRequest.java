package com.mercadolibre.planning.model.me.services.backlog;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.util.Set;
import lombok.Value;

@Value
public class BacklogRequest {
  String logisticCenterId;

  Set<Workflow> workflows;

  Set<ProcessName> processes;

  Instant dateFrom;

  Instant dateTo;

  Instant dateInFrom;

  Instant dateInTo;

  Instant slaFrom;

  Instant slaTo;

  Set<BacklogGrouper> groupBy;

}
