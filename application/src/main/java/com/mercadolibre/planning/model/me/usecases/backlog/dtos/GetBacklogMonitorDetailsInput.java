package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import lombok.Value;

@Value
public class GetBacklogMonitorDetailsInput {

  Instant requestDate;

  String warehouseId;

  Workflow workflow;

  ProcessName process;

  Instant dateFrom;

  Instant dateTo;

  Long callerId;

  boolean hasWall;
}
