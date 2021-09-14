package com.mercadolibre.planning.model.me.usecases.throughput;

import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
public class GetProcessThroughput implements UseCase<GetThroughputInput, GetThroughputResult> {

    private static final Map<ProcessName, List<ProcessName>> PROCESS_MAPPING = Map.of(
            WAVING, of(PICKING, PACKING, PACKING_WALL),
            PICKING, of(PICKING),
            BATCH_SORTER, of(BATCH_SORTER),
            WALL_IN, of(WALL_IN),
            PACKING, of(PACKING, PACKING_WALL),
            PACKING_WALL, of(PACKING_WALL),
            GLOBAL, of(GLOBAL)
    );

    private final PlanningModelGateway planningModelGateway;

    private final ThroughputResultMapper mapper;

    @Override
    public GetThroughputResult execute(GetThroughputInput input) {
        final Map<ProcessName, List<Entity>> entities =
                planningModelGateway.searchEntities(request(input))
                        .get(THROUGHPUT)
                        .stream()
                        .collect(Collectors.groupingBy(
                                Entity::getProcessName,
                                Collectors.toList()
                        ));

        return new GetThroughputResult(
                input.getProcesses()
                        .stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                process -> process.accept(mapper, entities)
                        ))
        );
    }

    private SearchEntitiesRequest request(GetThroughputInput input) {
        final List<ProcessName> processes = input.getProcesses()
                .stream()
                .flatMap(p -> PROCESS_MAPPING.get(p).stream())
                .distinct()
                .collect(Collectors.toList());

        return SearchEntitiesRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(FBM_WMS_OUTBOUND)
                .processName(processes)
                .entityTypes(of(THROUGHPUT))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .source(SIMULATION)
                .build();
    }
}
