package com.mercadolibre.planning.model.me.usecases.backlog;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.BACKLOG_UPPER_LIMIT;
import static java.time.ZoneOffset.UTC;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
class GetBacklogLimits {

  private final PlanningModelGateway planningModelGateway;

  public Map<ProcessName, Map<Instant, BacklogLimit>> execute(
      final GetBacklogLimitsInput input) {

    final Map<MagnitudeType, List<MagnitudePhoto>> entitiesByType =
        planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
                                                    .workflow(input.getWorkflow())
                                                    .entityTypes(of(BACKLOG_LOWER_LIMIT, BACKLOG_UPPER_LIMIT))
                                                    .warehouseId(input.getWarehouseId())
                                                    .dateFrom(input.getDateFrom().atZone(UTC))
                                                    .dateTo(input.getDateTo().atZone(UTC))
                                                    .processName(input.getProcesses())
                                                    .build()
        );

    final Map<ProcessName, Map<Instant, Integer>> lowerLimitsByProcessAndDate =
        entitiesByType.get(BACKLOG_LOWER_LIMIT)
            .stream()
            .collect(Collectors.groupingBy(
                MagnitudePhoto::getProcessName,
                Collectors.toMap(
                    entry -> entry.getDate().toInstant(),
                    MagnitudePhoto::getValue
                )
            ));

    final Map<ProcessName, List<MagnitudePhoto>> entitiesByProcess =
        entitiesByType.get(BACKLOG_UPPER_LIMIT)
            .stream()
            .collect(Collectors.groupingBy(
                MagnitudePhoto::getProcessName,
                Collectors.toList()
            ));

    return entitiesByProcess.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> buildProcess(
                lowerLimitsByProcessAndDate.get(entry.getKey()),
                entry.getValue()
            )
        ));
  }

  private Map<Instant, BacklogLimit> buildProcess(
      final Map<Instant, Integer> lowerLimits,
      final List<MagnitudePhoto> upperLimits) {

    return upperLimits.stream()
        .collect(Collectors.toMap(
            entity -> entity.getDate().toInstant(),
            entity -> new BacklogLimit(
                lowerLimits.get(entity.getDate().toInstant()),
                entity.getValue()
            )
        ));
  }
}
