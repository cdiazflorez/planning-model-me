package com.mercadolibre.planning.model.me.usecases.backlog.entities;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.ScheduleAdjustment;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class InboundBacklogMonitor {
  private Instant requestDate;
  private List<ScheduleAdjustment> scheduleAdjustments;
  private List<InboundBacklogScheduled> scheduled;
  private ProcessMetric checkIn;
  private ProcessMetric putAway;

  public InboundBacklogMonitor(
      final Instant requestDate,
      final List<InboundBacklogScheduled> scheduled,
      final ProcessMetric checkIn,
      final ProcessMetric putAway) {

    this.requestDate = requestDate;
    this.scheduled = scheduled;
    this.checkIn = checkIn;
    this.putAway = putAway;

  }
}
