package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.AREA;
import static com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper.STEP;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.NO_AREA;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.mergeMaps;
import static com.mercadolibre.planning.model.me.usecases.backlog.services.DetailsBacklogUtil.selectPhotos;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogPhotoApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Process;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.ProjectionGateway;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import com.mercadolibre.planning.model.me.usecases.backlog.GetBacklogMonitorDetails;
import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class DetailsBacklogService implements GetBacklogMonitorDetails.BacklogProvider {

  private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );

  private final BacklogPhotoApiGateway backlogPhotoApiGateway;

  private final ProjectionGateway projectionGateway;

  @Override
  public boolean canProvide(final ProcessName process) {
    return process != PICKING;
  }

  @Override
  public Map<Instant, List<NumberOfUnitsInAnArea>> getMonitorBacklog(final BacklogProviderInput input) {
    final var process = Process.from(input.getProcess());
    final var backlog = getBacklog(input, process);
    final var mappedBacklog = mapBacklogToNumberOfUnitsInAreasByTakenOn(backlog.get(process));

    final var pastBacklog = selectPhotos(mappedBacklog, input.getDateFrom(), input.getRequestDate());
    final var projectedBacklog = getProjectedBacklog(input, pastBacklog);

    return mergeMaps(pastBacklog, projectedBacklog);
  }

  private Map<Process, List<BacklogPhoto>> getBacklog(final BacklogProviderInput input, final Process process) {
    return backlogPhotoApiGateway.getTotalBacklogPerProcessAndInstantDate(
        new BacklogRequest(
            input.getWarehouseId(),
            Set.of(input.getWorkflow()),
            Set.of(process),
            input.getDateFrom(),
            input.getRequestDate(),
            null,
            null,
            input.getSlaFrom(),
            input.getSlaTo(),
            Set.of(STEP)
        )
    );
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> mapBacklogToNumberOfUnitsInAreasByTakenOn(final List<BacklogPhoto> backlog) {
    return backlog.stream()
        .collect(Collectors.toMap(
            BacklogPhoto::getTakenOn,
            photo -> of(
                new NumberOfUnitsInAnArea(photo.getGroupValue(AREA).orElse(NO_AREA), photo.getQuantity())
            )
        ));
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklog(final BacklogProviderInput input,
                                                                        final Map<Instant, List<NumberOfUnitsInAnArea>> backlog) {

    final var lastConsolidationDate = backlog.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
    final var currentBacklog = backlog.entrySet()
        .stream()
        .filter(entry -> lastConsolidationDate.equals(entry.getKey()))
        .map(Map.Entry::getValue)
        .flatMap(List::stream)
        .mapToInt(NumberOfUnitsInAnArea::getUnits)
        .sum();

    // the projection requires backlog for all processes
    final var currentBacklogAtProcess = PROCESS_BY_WORKFLOWS.get(input.getWorkflow())
        .stream()
        .map(process -> new CurrentBacklog(
            process,
            input.getProcess() == process ? currentBacklog : 0)
        )
        .collect(Collectors.toList());

    try {
      return getProjectedBacklogWithoutAreas(input, currentBacklogAtProcess);
    } catch (RuntimeException e) {
      log.error("could not retrieve backlog projections", e);
    }

    return emptyMap();
  }

  private Map<Instant, List<NumberOfUnitsInAnArea>> getProjectedBacklogWithoutAreas(final BacklogProviderInput input,
                                                                                    final List<CurrentBacklog> backlog) {

    final Instant dateFrom = input.getRequestDate().truncatedTo(ChronoUnit.HOURS);

    final Instant dateTo = input.getDateTo()
        .truncatedTo(ChronoUnit.HOURS);

    final List<BacklogProjectionResponse> projectedBacklog = projectionGateway.getBacklogProjection(
        BacklogProjectionRequest.builder()
            .warehouseId(input.getWarehouseId())
            .workflow(input.getWorkflow())
            .processName(of(input.getProcess()))
            .dateFrom(dateFrom.atZone(UTC))
            .dateTo(dateTo.atZone(UTC))
            .currentBacklog(backlog)
            .applyDeviation(true)
            .build()
    );

    return projectedBacklog.stream()
        .filter(projection -> projection.getProcessName().equals(input.getProcess()))
        .findFirst()
        .map(projection -> projection.getValues()
            .stream()
            .collect(
                Collectors.toMap(
                    projectionValue -> projectionValue.getDate().toInstant(),
                    projectionValue -> of(
                        new NumberOfUnitsInAnArea(
                            NO_AREA,
                            projectionValue.getQuantity()
                        )
                    )
                ))
        )
        .orElseGet(Collections::emptyMap);
  }

}
