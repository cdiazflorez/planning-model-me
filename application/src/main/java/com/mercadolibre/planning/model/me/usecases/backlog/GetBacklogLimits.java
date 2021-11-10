package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetBacklogLimitsInput;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.BACKLOG_UPPER_LIMIT;
import static java.time.ZoneOffset.UTC;
import static java.util.List.of;

@Named
@AllArgsConstructor
class GetBacklogLimits {

    private final PlanningModelGateway planningModelGateway;

    public Map<ProcessName, Map<Instant, BacklogLimit>> execute(
            final GetBacklogLimitsInput input) {

        final Map<EntityType, List<Entity>> entitiesByType =
                planningModelGateway.searchEntities(SearchEntitiesRequest.builder()
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
                                Entity::getProcessName,
                                Collectors.toMap(
                                        entry -> entry.getDate().toInstant(),
                                        Entity::getValue
                                )
                        ));

        final Map<ProcessName, List<Entity>> entitiesByProcess =
                entitiesByType.get(BACKLOG_UPPER_LIMIT)
                        .stream()
                        .collect(Collectors.groupingBy(
                                Entity::getProcessName,
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
            final List<Entity> upperLimits) {

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
