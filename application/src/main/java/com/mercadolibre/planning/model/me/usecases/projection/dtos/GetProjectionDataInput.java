package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetProjectionDataInput {
  final ZonedDateTime dateFrom;

  final ZonedDateTime dateTo;

  final boolean showDeviation;

  final List<Backlog> sales;

  final List<PlanningDistributionResponse> planningDistribution;

  final List<ProjectionResult> projections;

  final List<Backlog> backlogs;

  private Workflow workflow;

  private String warehouseId;
}
