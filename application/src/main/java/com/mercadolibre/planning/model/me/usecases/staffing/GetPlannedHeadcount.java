package com.mercadolibre.planning.model.me.usecases.staffing;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.HOUR_MINUTES_FORMATTER;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToTimeZone;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;

import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcount;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByHour;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByProcess;
import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcountByWorkflow;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.exception.NoPlannedDataException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetPlannedHeadcountInput;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.PlannedEntity;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Named
@AllArgsConstructor
@Slf4j
public class GetPlannedHeadcount implements UseCase<GetPlannedHeadcountInput, PlannedHeadcount> {

  private final LogisticCenterGateway logisticCenterGateway;
  private final PlanningModelGateway planningModelGateway;

  @Override
  public PlannedHeadcount execute(final GetPlannedHeadcountInput input) {
    final ZoneId zoneId = logisticCenterGateway
        .getConfiguration(input.getLogisticCenterId()).getZoneId();
    final ZonedDateTime dateTimeFrom = ZonedDateTime.now(zoneId).truncatedTo(DAYS);
    final ZonedDateTime dateTimeTo = dateTimeFrom.plusHours(23);
    Map<MagnitudeType, List<MagnitudePhoto>> entities;

    try {
      entities = planningModelGateway.searchTrajectories(
          SearchTrajectoriesRequest.builder()
              .warehouseId(input.getLogisticCenterId())
              .workflow(FBM_WMS_OUTBOUND)
              .entityTypes(List.of(HEADCOUNT, THROUGHPUT))
              .dateFrom(dateTimeFrom)
              .dateTo(dateTimeTo)
              .processName(List.of(PICKING, PACKING, PACKING_WALL))
              .entityFilters(Map.of(
                  HEADCOUNT, Map.of(
                      PROCESSING_TYPE.toJson(),
                      List.of(
                              ACTIVE_WORKERS.getName(),
                              EFFECTIVE_WORKERS.getName()
                      )
                  )
              ))
              .build()
      );
    } catch (Exception ex) {
      throw new NoPlannedDataException(ex);
    }

    final List<PlannedEntity> headcount = entities.get(HEADCOUNT).stream()
        .map((MagnitudePhoto entity) -> PlannedEntity.fromEntity(entity, HEADCOUNT))
        .collect(Collectors.toList());

    final List<PlannedEntity> throughput = entities.get(THROUGHPUT).stream()
        .map((MagnitudePhoto entity) -> PlannedEntity.fromEntity(entity, THROUGHPUT))
        .collect(Collectors.toList());

    final Map<ZonedDateTime, Map<Workflow, Map<ProcessName, List<PlannedEntity>>>>
        groupedEntities = Stream.of(headcount, throughput).flatMap(Collection::stream)
        .collect(
            groupingBy(PlannedEntity::getDate,
                       groupingBy(PlannedEntity::getWorkflow,
                                  groupingBy(PlannedEntity::getProcessName)
                       )
            )
        );

    return createResponseFrom(groupedEntities, zoneId);
  }

  private PlannedHeadcount createResponseFrom(
      final Map<ZonedDateTime, Map<Workflow, Map<ProcessName, List<PlannedEntity>>>>
          groupedEntities,
      final ZoneId zoneId) {
    final List<PlannedHeadcountByHour> plannedHeadcountByHours = groupedEntities.keySet()
        .stream()
        .map(dateTime -> new PlannedHeadcountByHour(
            convertToTimeZone(zoneId, dateTime)
                .format(HOUR_MINUTES_FORMATTER),
            getPlannedWorkflows(groupedEntities.get(dateTime))
        ))
        .sorted(Comparator.comparing(PlannedHeadcountByHour::getHour))
        .collect(Collectors.toList());

    return new PlannedHeadcount(plannedHeadcountByHours);
  }

  private List<PlannedHeadcountByWorkflow> getPlannedWorkflows(
      final Map<Workflow, Map<ProcessName, List<PlannedEntity>>> workflowMap) {
    return workflowMap.keySet().stream()
        .map(workflow -> {
          final Map<ProcessName, List<PlannedEntity>> map = workflowMap.get(workflow);
          return new PlannedHeadcountByWorkflow(
              workflow.getName(), getTotal(map), getEntitiesByProcess(map)
          );
        })
        .collect(Collectors.toList());
  }

  private Integer getTotal(final Map<ProcessName, List<PlannedEntity>> map) {
    return map.values().stream()
        .flatMap(Collection::stream)
        .filter(plannedEntity -> HEADCOUNT == plannedEntity.getType())
        .map(PlannedEntity::getValue)
        .mapToInt(Integer::intValue)
        .sum();
  }

  private List<PlannedHeadcountByProcess> getEntitiesByProcess(
      final Map<ProcessName, List<PlannedEntity>> map) {
    return map.entrySet().stream()
        .map(entry -> createProcessMetrics(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(PlannedHeadcountByProcess::getProcess))
        .collect(Collectors.toList());
  }

  private PlannedHeadcountByProcess createProcessMetrics(
      final ProcessName processName, final List<PlannedEntity> plannedEntities) {
    final Integer totalWorkers = plannedEntities.stream()
        .filter(plannedEntity -> HEADCOUNT == plannedEntity.getType())
        .findFirst()
        .map(PlannedEntity::getValue)
        .orElse(0);

    final Integer throughput = plannedEntities.stream()
        .filter(plannedEntity -> THROUGHPUT == plannedEntity.getType())
        .distinct()
        .findFirst()
        .map(PlannedEntity::getValue)
        .orElse(0);

    return new PlannedHeadcountByProcess(processName.getName(), totalWorkers, throughput);
  }
}
