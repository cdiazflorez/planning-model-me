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

  private static double getNumericDeviation(final long backlogQuantity,
                                            final long forecastedItemsForCpt) {

    if (forecastedItemsForCpt == 0 || backlogQuantity == 0) {
      return 0.0;
    }

    return (((double) backlogQuantity / forecastedItemsForCpt) - 1);
  }

  private static long filterForecastQuantity(final ZonedDateTime cpt,
                                             final List<PlanningDistributionResponse> planning) {
    return planning.stream()
        .filter(elem -> cpt.isEqual(elem.getDateOut()))
        .mapToLong(PlanningDistributionResponse::getTotal)
        .sum();
  }

  private static long filterBacklogQuantity(final ZonedDateTime cpt, final List<Backlog> backlogs) {
    return backlogs.stream()
        .filter(backlog -> cpt.isEqual(backlog.getDate()))
        .mapToLong(Backlog::getQuantity)
        .sum();
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
    final long backlog = filterBacklogQuantity(cpt, backlogs);
    final long soldItems = filterBacklogQuantity(cpt, sales);

    final Long forecastItems = showDeviation ? filterForecastQuantity(cpt, planningDistribution) : null;
    final Double divertedItems = showDeviation ? getNumericDeviation(soldItems, forecastItems) : null;

    final int processingTime = Optional.ofNullable(projection.getProcessingTime())
        .map(ProcessingTime::getValue).orElse(DEFAULT_PROCESSING_TIME);

    return new Projection(
        cpt.toInstant(),
        projectedEndDate,
        backlog,
        forecastItems,
        soldItems,
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
