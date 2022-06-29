package com.mercadolibre.planning.model.me.usecases.backlog.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class GetBacklogMonitorInputDto {

  Instant requestDate;

  String warehouseId;

  Workflow workflow;

  List<ProcessName> processes;

  Instant dateFrom;

  Instant dateTo;

  Long callerId;
}
