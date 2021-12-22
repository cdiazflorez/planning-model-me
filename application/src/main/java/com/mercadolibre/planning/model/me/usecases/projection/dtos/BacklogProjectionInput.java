package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class BacklogProjectionInput {

    Workflow workflow;

    String warehouseId;

    List<ProcessName> processName;

    long userId;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    String groupType;

    List<Consolidation> currentBacklog;
}
