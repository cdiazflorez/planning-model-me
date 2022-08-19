package com.mercadolibre.planning.model.me.usecases.projection;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionDataInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public final class ProjectionDataMapper {
  private static final int DEFAULT_PROCESSING_TIME = 240;

  private ProjectionDataMapper() {
    throw new UnsupportedOperationException("Utility class and cannot be instantiated");
  }

  public static List<Projection> map(final GetProjectionDataInput input) {
    return input.getProjections().stream()
        .sorted(Comparator.comparing(ProjectionResult::getDate).reversed())
        .map(projection -> createProjection(
            input.getBacklogs(),
            input.getSales(),
            input.getPlanningDistribution(),
            projection,
            input.isShowDeviation())
        )
        .collect(toList());
  }

  private static double getNumericDeviation(final ZonedDateTime cpt,
                                            final Integer backlogQuantity,
                                            final List<PlanningDistributionResponse> planning) {
    final long forecastedItemsForCpt = planning.stream()
        .filter(distribution -> cpt.isEqual(distribution.getDateOut()))
        .mapToLong(PlanningDistributionResponse::getTotal)
        .sum();

    if (forecastedItemsForCpt == 0 || backlogQuantity == 0) {
      return 0.0;
    }

    return (((double) backlogQuantity / forecastedItemsForCpt) - 1);
  }

  private static Long filterForecastQuantity(final ZonedDateTime cpt,
                                             final List<PlanningDistributionResponse> planning) {
    final Optional<PlanningDistributionResponse> cptForecast = planning.stream()
        .filter(elem -> cpt.isEqual(elem.getDateOut()))
        .findFirst();

    return cptForecast.map(PlanningDistributionResponse::getTotal).orElse(0L);
  }

  private static int filterBacklogQuantity(final ZonedDateTime cpt, final List<Backlog> backlogs) {
    final Optional<Backlog> cptBacklog = backlogs.stream()
        .filter(backlog -> cpt.isEqual(backlog.getDate()))
        .findFirst();

    return cptBacklog.map(Backlog::getQuantity).orElse(0);
  }

  private static Projection createProjection(
      final List<Backlog> backlogs,
      final List<Backlog> sales,
      final List<PlanningDistributionResponse> planningDistribution,
      final ProjectionResult projection,
      final boolean showDeviation
  ) {
    final ZonedDateTime cpt = projection.getDate();
    final Instant projectedEndDate = projection.getProjectedEndDate() != null
        ? projection.getProjectedEndDate().toInstant()
        : null;
    final Instant simulatedEndDate = projection.getSimulatedEndDate() != null
        ? projection.getSimulatedEndDate().toInstant()
        : null;
    final int backlog = filterBacklogQuantity(cpt, backlogs);
    final Integer soldItems = filterBacklogQuantity(cpt, sales);

    final Long forecastItems = showDeviation ? filterForecastQuantity(cpt, planningDistribution) : null;
    final Double divertedItems = showDeviation ? getNumericDeviation(cpt, soldItems, planningDistribution) : null;

    final int processingTime = Optional.ofNullable(projection.getProcessingTime())
        .map(ProcessingTime::getValue).orElse(DEFAULT_PROCESSING_TIME);

    return new Projection(
        cpt.toInstant(),
        projectedEndDate,
        backlog,
        forecastItems,
        processingTime,
        projection.getRemainingQuantity(),
        projection.isDeferred(),
        projection.isExpired(),
        divertedItems,
        simulatedEndDate,
        projection.getDeferredAt(),
        projection.getDeferredUnits(),
        projection.getDeferralStatus()
    );
  }

}
