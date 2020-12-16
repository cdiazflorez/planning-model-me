package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class BacklogProjectionRequest {

    Workflow workflow;

    String warehouseId;

    List<ProcessName> processName;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    List<ProcessBacklog> currentBacklog;
}
