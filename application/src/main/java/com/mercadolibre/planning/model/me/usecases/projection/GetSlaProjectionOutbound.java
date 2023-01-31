package com.mercadolibre.planning.model.me.usecases.projection;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.services.projection.CalculateProjectionService;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetProjectionInput;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements method for outbound SLAs projection.
 *
 * <p>
 * Its responsibility is to call the run simulation method on planning gateway.
 * </p>
 */
@Slf4j
@Named
public class GetSlaProjectionOutbound extends GetProjectionOutbound {

  private final FeatureSwitches featureSwitches;

  private final CalculateProjectionService calculateProjection;

  protected GetSlaProjectionOutbound(final PlanningModelGateway planningModelGateway,
                                     final LogisticCenterGateway logisticCenterGateway,
                                     final GetEntities getEntities,
                                     final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                     final BacklogApiGateway backlogGateway,
                                     final GetSales getSales,
                                     final FeatureSwitches featureSwitches,
                                     final CalculateProjectionService calculateProjection,
                                     final RatioService ratioService) {

    super(planningModelGateway, logisticCenterGateway, getEntities, getSimpleDeferralProjection, backlogGateway,
        getSales, ratioService);
    this.featureSwitches = featureSwitches;
    this.calculateProjection = calculateProjection;
  }


  @Override
  protected List<ProjectionResult> getProjection(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Backlog> backlogs,
                                                 final LogisticCenterConfiguration config) {


    final var currentBacklog = getCurrentBacklog(input, dateFrom, dateTo);

    final var deferralProjectionOutput = this.getSimpleDeferralProjection.execute(
        new GetProjectionInput(
            input.getWarehouseId(),
            input.getWorkflow(),
            input.getDate(),
            backlogs,
            false,
            emptyList())).getProjections();


    final var deferredStatusBySla = deferralProjectionOutput.stream()
        .collect(toMap(ProjectionResult::getDate, ProjectionResult::isDeferred, (a, b) -> a, TreeMap::new));


    final var plannedBacklog = getExpectedBacklog(input.getWarehouseId(), input.getWorkflow(), dateFrom, dateTo);


    final var filteredPlannedBacklogByDeferralStatus = getFilteredPlannedBacklogByDeferralStatus(
        deferredStatusBySla,
        plannedBacklog
    );

    final var defaultSlas = getSlas(input, dateFrom, dateTo, currentBacklog, plannedBacklog, config.getTimeZone().getID());
    final var packingRatio = getPackingRatio(
        input.getWarehouseId(),
        config.isPutToWall(),
        input.getRequestDate(),
        dateTo.toInstant().plus(2, HOURS),
        dateFrom.toInstant(),
        dateTo.toInstant()
    );

    final List<ProjectionResult> projectionsSla = calculateProjection.execute(
            input.getRequestDate(),
            Instant.from(dateFrom),
            Instant.from(dateTo),
            input.getWorkflow(),
            getThroughputByProcess(input, dateFrom, dateTo, emptyList()),
            currentBacklog,
            filteredPlannedBacklogByDeferralStatus,
            defaultSlas,
            packingRatio
        )
        .stream()
        .sorted(Comparator.comparing(ProjectionResult::getDate))
        .collect(toList());

    return getProjectionsWithItsDeferralStatus(projectionsSla, deferralProjectionOutput);
  }

  private List<ProjectionResult> getProjectionsWithItsDeferralStatus(final List<ProjectionResult> slaProjections,
                                                                     final List<ProjectionResult> deferralProjections) {

    final Map<Instant, ProjectionResult> deferralProjectionsByDateOut =
        deferralProjections.stream().collect(toMap(
            pt -> pt.getDate().toInstant(),
            Function.identity(),
            (pd1, pd2) -> pd2
        ));

    final List<ProjectionResult> newSlaProjections = new ArrayList<>(slaProjections);

    for (ProjectionResult slaProjection : newSlaProjections) {
      final ProjectionResult deferralProjection =
          deferralProjectionsByDateOut.get(slaProjection.getDate().toInstant());
      if (deferralProjection != null) {
        slaProjection.setDeferred(deferralProjection.isDeferred());
      } else {
        log.info("Not found cptProjection [{}] in cptDeferral", slaProjection.getDate()
            .toInstant());
      }
    }

    return newSlaProjections;
  }
}
