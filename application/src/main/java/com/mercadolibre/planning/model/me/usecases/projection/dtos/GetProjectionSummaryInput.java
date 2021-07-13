package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetProjectionSummaryInput {

    private Workflow workflow;

    private String warehouseId;

    final ZonedDateTime dateFrom;

    final ZonedDateTime dateTo;

    final boolean showDeviation;

    final List<ProjectionResult> projections;

    final List<Backlog> backlogs;
}
