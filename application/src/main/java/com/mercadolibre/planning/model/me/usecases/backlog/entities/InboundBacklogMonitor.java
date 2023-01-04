package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class InboundBacklogMonitor {
  Instant requestDate;
  List<InboundBacklogScheduled> scheduled;
  ProcessMetric checkIn;
  ProcessMetric putAway;
}
