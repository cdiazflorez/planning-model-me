package com.mercadolibre.planning.model.me.usecases.projection;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.projection.ProjectionWorkflow.getSteps;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.chart.ProcessingTime;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.CycleTimeRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator.PackingRatio;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import com.mercadolibre.planning.model.me.usecases.projection.deferral.GetSimpleDeferralProjection;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import com.mercadolibre.planning.model.me.usecases.sales.GetSales;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements template method for outbound SLAs projections and simulations.
 *
 * <p>
 * Includes common behaviour for outbound projections as retrieving backlog, mapping projections and building responses.
 * </p>
 */
@Slf4j
public abstract class GetProjectionOutbound extends GetProjection {

  final GetSimpleDeferralProjection getSimpleDeferralProjection;

  private final BacklogApiGateway backlogGateway;

  private final RatioService ratioService;

  protected GetProjectionOutbound(final PlanningModelGateway planningModelGateway,
                                  final LogisticCenterGateway logisticCenterGateway,
                                  final GetEntities getEntities,
                                  final GetSimpleDeferralProjection getSimpleDeferralProjection,
                                  final BacklogApiGateway backlogGateway,
                                  final GetSales getSales,
                                  final RatioService ratioService) {

    super(getSales, planningModelGateway, logisticCenterGateway, getEntities);

    this.getSimpleDeferralProjection = getSimpleDeferralProjection;
    this.backlogGateway = backlogGateway;
    this.ratioService = ratioService;
  }

  protected Map<ProcessName, Map<Instant, Integer>> getThroughputByProcess(final GetProjectionInputDto input,
                                                                           final ZonedDateTime dateFrom,
                                                                           final ZonedDateTime dateTo,
                                                                           final List<Simulation> simulations) {

    final var magnitudes = planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processName(of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL))
        .entityTypes(of(THROUGHPUT))
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .source(SIMULATION)
        .simulations(simulations)
        .build()).get(THROUGHPUT);

    return magnitudes.stream()
        .collect(Collectors.groupingBy(
                MagnitudePhoto::getProcessName,
                Collectors.toMap(
                    entry -> entry.getDate().toInstant(),
                    MagnitudePhoto::getValue)
            )
        );
  }

  @Override
  protected final List<Backlog> getBacklog(final Workflow workflow,
                                           final String warehouseId,
                                           final Instant dateFromToProject,
                                           final Instant dateToToProject,
                                           final ZoneId zoneId,
                                           final Instant requestDate) {

    final var lastPhoto = backlogGateway.getLastPhoto(
        new BacklogLastPhotoRequest(
            warehouseId,
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            getSteps(FBM_WMS_OUTBOUND),
            null,
            null,
            dateFromToProject,
            dateToToProject,
            Set.of(DATE_OUT),
            Instant.now()
        )
    );

    return lastPhoto == null
        ? emptyList()
        : lastPhoto.getGroups().stream()
        .map(photo -> new Backlog(
            ZonedDateTime.parse(photo.getKey().get(DATE_OUT)),
            photo.getTotal()
        )).collect(toList());
  }

  protected List<Photo.Group> getCurrentBacklog(final GetProjectionInputDto input,
                                                final ZonedDateTime dateFrom,
                                                final ZonedDateTime dateTo) {

    return backlogGateway.getLastPhoto(new BacklogLastPhotoRequest(
        input.getWarehouseId(),
        Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
        getSteps(FBM_WMS_OUTBOUND),
        null,
        null,
        dateFrom.toInstant(),
        dateTo.toInstant(),
        Set.of(STEP, DATE_OUT, AREA),
        input.getRequestDate()
    )).getGroups();
  }

  protected List<PlanningDistributionResponse> getExpectedBacklog(final String warehouseId,
                                                                  final Workflow workflow,
                                                                  final ZonedDateTime dateFrom,
                                                                  final ZonedDateTime dateTo) {

    return planningModelGateway.getPlanningDistribution(PlanningDistributionRequest.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateInFrom(dateFrom)
        .dateInTo(dateTo)
        .dateOutFrom(dateFrom)
        .dateOutTo(dateTo)
        .applyDeviation(true)
        .build());
  }

  protected Map<Instant, ProcessingTime> getSlas(final GetProjectionInputDto input,
                                                 final ZonedDateTime dateFrom,
                                                 final ZonedDateTime dateTo,
                                                 final List<Photo.Group> currentBacklog,
                                                 final List<PlanningDistributionResponse> plannedBacklog,
                                                 final String timeZone) {

    final List<ZonedDateTime> currentBacklogSlas = currentBacklog.stream()
        .map(item -> ZonedDateTime.parse(item.getGroupValue(DATE_OUT).orElseThrow())).collect(toList());

    final List<ZonedDateTime> plannedBacklogSlas = plannedBacklog.stream()
        .map(PlanningDistributionResponse::getDateOut).collect(toList());

    currentBacklogSlas.addAll(plannedBacklogSlas);

    var cycleTimeValues = planningModelGateway.getCycleTime(
        input.getWarehouseId(),
        CycleTimeRequest.builder()
            .workflows(Set.of(FBM_WMS_OUTBOUND))
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .slas(currentBacklogSlas.stream().distinct().collect(toList()))
            .timeZone(timeZone)
            .build()
    );

    return cycleTimeValues.get(FBM_WMS_OUTBOUND).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, item -> new ProcessingTime((int) item.getValue().getCycleTime(), MINUTES.getName())));
  }

  protected Map<Instant, PackingRatio> getPackingRatio(final String logisticCenterId,
                                                       final boolean hasWall,
                                                       final Instant slaDateFrom,
                                                       final Instant slaDateTo,
                                                       final Instant dateFrom,
                                                       final Instant dateTo) {

    if (hasWall) {
      return ratioService.getPackingRatio(logisticCenterId, slaDateFrom, slaDateTo, dateFrom, dateTo);
    } else {
      return getPickingRatioForTotePacking(dateFrom, dateTo);
    }

  }

  private Map<Instant, PackingRatio> getPickingRatioForTotePacking(final Instant dateFrom, final Instant dateTo) {
    final long hoursBetweenDates = Duration.between(dateFrom, dateTo).toHours();

    return LongStream.rangeClosed(0, hoursBetweenDates)
        .boxed()
        .collect(Collectors.toMap(
            i -> dateFrom.plus(i, ChronoUnit.HOURS),
            i -> new PackingRatio(1.0, 0.0)));
  }
}

