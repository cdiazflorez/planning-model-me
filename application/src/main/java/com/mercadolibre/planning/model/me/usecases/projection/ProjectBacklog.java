package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest.fromInput;
import static java.time.temporal.ChronoUnit.HOURS;

import com.mercadolibre.planning.model.me.gateways.entity.EntityGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.ProjectedBacklogForAnAreaAndOperatingHour;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.entities.ProjectedBacklog;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class ProjectBacklog {

  private final PlanningModelGateway planningModel;

  private final ProjectionGateway projectionGateway;

  private final EntityGateway entityGateway;

  public ProjectedBacklog execute(final BacklogProjectionInput input) {
    return new ProjectedBacklog(planningModel.getBacklogProjection(fromInput(input)));
  }

  public List<ProjectedBacklogForAnAreaAndOperatingHour> projectBacklogInAreas(final Instant dateFrom,
                                                                               final Instant dateTo,
                                                                               final String warehouseId,
                                                                               final Workflow workflow,
                                                                               final List<ProcessName> processes,
                                                                               final List<BacklogQuantityAtSla> backlog,
                                                                               final List<MagnitudePhoto> throughput) {

    final Instant dateToInclusive = dateTo.plus(1, HOURS);
    final List<PlanningDistributionResponse> plannedUnits = getPlannedUnits(dateFrom, dateToInclusive, warehouseId, workflow);

    final Instant lastSla = calculateLastSla(backlog, plannedUnits, dateToInclusive);
    final List<BacklogAreaDistribution> shareDistributions = getShareDistribution(dateFrom, lastSla, warehouseId, workflow);

    return projectionGateway.projectBacklogInAreas(
        dateFrom,
        dateToInclusive,
        workflow,
        processes,
        backlog,
        plannedUnits,
        throughput,
        shareDistributions
    );
  }

  private List<PlanningDistributionResponse> getPlannedUnits(final Instant dateFrom,
                                                             final Instant dateTo,
                                                             final String warehouseId,
                                                             final Workflow workflow) {

    final var planningDistributionDateFrom = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final var planningDistributionDateTo = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC);

    // TODO: the `plusDays(1L)` is based on planning-api backlog projection
    return planningModel.getPlanningDistribution(
        new PlanningDistributionRequest(
            warehouseId,
            workflow,
            planningDistributionDateFrom,
            planningDistributionDateTo.plusDays(1L),
            planningDistributionDateFrom,
            planningDistributionDateTo.plusDays(1L),
            true
        )
    );
  }

  private List<BacklogAreaDistribution> getShareDistribution(final Instant dateFrom,
                                                             final Instant dateTo,
                                                             final String warehouseId,
                                                             final Workflow workflow) {

    final var dateFromWithZone = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final var dateToWithZone = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC).plusHours(1);

    final GetShareDistributionInput input = new GetShareDistributionInput(dateFromWithZone, dateToWithZone, warehouseId);
    return entityGateway.getShareDistribution(input, workflow)
        .stream()
        .map(share ->
            new BacklogAreaDistribution(
                ProcessName.valueOf(share.getProcessName().toUpperCase(Locale.ENGLISH)),
                share.getDate().toInstant(),
                share.getArea(),
                share.getQuantity()
            )
        ).collect(Collectors.toList());
  }

  private Instant calculateLastSla(final List<BacklogQuantityAtSla> backlog,
                                   final List<PlanningDistributionResponse> plannedUnits,
                                   final Instant defaultValue) {

    final var backlogMax = backlog.stream()
        .map(BacklogQuantityAtSla::getDateOut)
        .max(Comparator.naturalOrder())
        .orElse(defaultValue);

    final var plannedMax = plannedUnits.stream()
        .map(PlanningDistributionResponse::getDateOut)
        .map(ZonedDateTime::toInstant)
        .max(Comparator.naturalOrder())
        .orElse(defaultValue);

    return backlogMax.isAfter(plannedMax) ? backlogMax : plannedMax;
  }

}
