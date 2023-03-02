package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import java.time.Instant;
import lombok.Value;

@Value
public class InboundBacklogScheduled {

  Instant dateFrom;

  Instant dateTo;

  BacklogScheduledMetrics inbound;

  BacklogScheduledMetrics inboundTransfer;

  BacklogScheduledMetrics total;

  long deviationAdjustment;

}
