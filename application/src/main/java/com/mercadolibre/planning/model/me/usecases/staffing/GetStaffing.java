package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Area;
import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Worker;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchEntitiesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.Aggregation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.GetStaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.Operation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Result;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class GetStaffing implements UseCase<GetStaffingInput, Staffing> {

    private static final int AREA_INDEX = 3;

    private final PlanningModelGateway planningModelGateway;

    private final StaffingGateway staffingGateway;

    //TODO: Unificar esta config que esta repetida en staffing-api
    private static final Map<String, List<String>> WORKFLOWS = Map.of(
            "fbm-wms-outbound", List.of(
                    "picking", "batch_sorter", "wall_in", "packing", "packing_wall"
            ),
            "fbm-wms-inbound", List.of("receiving", "check_in", "put_away")
    );

    @Override
    public Staffing execute(final GetStaffingInput input) {

        final String logisticCenterId = input.getLogisticCenterId();
        final ZonedDateTime now = getCurrentUtcDateTime();

        final List<Result> lastHourMetrics = getStaffingMetrics(logisticCenterId, now, 60);
        final List<Result> lastMinutesMetrics = getStaffingMetrics(logisticCenterId, now, 11);

        return mapMetricsResults(logisticCenterId, now, lastHourMetrics, lastMinutesMetrics);
    }

    private Staffing mapMetricsResults(final String logisticCenterId,
                                       final ZonedDateTime now,
                                       final List<Result> productivityMetrics,
                                       final List<Result> quantityMetrics) {

        final List<StaffingWorkflow> workflows = new ArrayList<>();

        WORKFLOWS.forEach((workflow, processNames) -> {
            final Map<EntityType, List<Entity>> forecastStaffing;
            //TODO: Eliminar este IF cuando planning model api devuelva otros workflows
            if ("fbm-wms-outbound".equals(workflow)) {
                forecastStaffing = getForecastStaffing(logisticCenterId,
                        processNames, now);
            } else {
                forecastStaffing = Map.of(EntityType.PRODUCTIVITY, Collections.emptyList());
            }
            final List<Process> processes = processNames.stream().map(p -> {
                final Worker lastMinutesWorker = calculateWorkersQty(quantityMetrics, workflow, p);
                final Worker lastHourWorker = calculateWorkersQty(productivityMetrics, workflow, p);
                final Integer productivity =
                        calculateNetProductivity(productivityMetrics, workflow, p);

                return Process.builder()
                        .process(p)
                        .netProductivity(productivity)
                        .workers(lastMinutesWorker)
                        .areas(createAreas(quantityMetrics, workflow, p))
                        .throughput(productivity * lastHourWorker.getBusy())
                        .targetProductivity(filterProductivity(forecastStaffing, p))
                        .build();
            }).collect(toList());

            workflows.add(StaffingWorkflow.builder()
                    .workflow(workflow)
                    .processes(processes)
                    .totalWorkers(calculateTotalWorkers(processes))
                    .totalNonSystemicWorkers(
                            calculateNonSystemicWorkersQty(quantityMetrics, workflow))
                    .build());
        });

        return Staffing.builder()
                .totalWorkers(workflows.stream().mapToInt(w -> w.getTotalWorkers()).sum())
                .workflows(workflows).build();
    }

    private Integer calculateTotalWorkers(final List<Process> processes) {
        return processes.stream().mapToInt(process ->
                process.getWorkers().getBusy() + process.getWorkers().getIdle())
                .sum();
    }

    private Integer calculateNetProductivity(final List<Result> results,
                                             final String workflow,
                                             final String process) {
        return (int) findResultByKey(results, workflow, process, "working_systemic").stream()
                .mapToInt(result -> result.getResult("net_productivity"))
                .average()
                .orElse(0);
    }

    private Integer calculateNonSystemicWorkersQty(final List<Result> results,
                                                   final String workflow) {

        return calculateWorkersQtyByKeys(results, workflow, "working_non_systemic");
    }

    private Worker calculateWorkersQty(final List<Result> results,
                                       final String workflow,
                                       final String process) {

        final Integer idle = calculateWorkersQtyByKeys(results,
                workflow, process, "idle");

        final Integer working = calculateWorkersQtyByKeys(results,
                workflow, process, "working_systemic");

        return new Worker(idle, working);
    }

    private Integer calculateWorkersQtyByKeys(final List<Result> results, final String... keys) {

        return findResultByKey(results, keys).stream()
                .mapToInt(result -> result.getResult("total_workers"))
                .sum();
    }

    private List<Area> createAreas(final List<Result> results,
                                   final String workflow,
                                   final String process) {

        return findResultByKey(results, workflow, process, "working_systemic").stream()
                .filter(result -> !result.getKeys().get(AREA_INDEX).equals(""))
                .map(result -> new Area(
                        result.getKeys().get(AREA_INDEX),
                        result.getResult("effective_productivity"),
                        new Worker(null, result.getResult("total_workers"))
                )).collect(toList());
    }

    private List<Result> findResultByKey(final List<Result> results, final String... keys) {
        return results.stream()
                .filter(result -> result.getKeys().containsAll(List.of(keys)))
                .collect(toList());
    }

    private List<Result> getStaffingMetrics(final String logisticCenterId,
                                            final ZonedDateTime now,
                                            final long minutes) {

        final List<Result> results = staffingGateway.getStaffing(new GetStaffingRequest(
                now.minusMinutes(minutes),
                now,
                logisticCenterId,
                List.of(new Aggregation(
                        "staffing",
                        List.of("workflow", "process", "worker_status", "area"),
                        List.of(
                                new Operation("total_workers", "worker_id", "count"),
                                new Operation("net_productivity", "net_productivity", "avg"),
                                new Operation(
                                        "effective_productivity",
                                        "effective_productivity",
                                        "avg"))
                )))
        ).getAggregations().get(0).getResults();

        return results != null ? results : new ArrayList<>();
    }

    private Map<EntityType, List<Entity>> getForecastStaffing(final String logisticCenterId,
                                                              final List<String> processes,
                                                              final ZonedDateTime now) {

        try {
            return planningModelGateway.searchEntities(SearchEntitiesRequest.builder()
                    .warehouseId(logisticCenterId)
                    .workflow(Workflow.FBM_WMS_OUTBOUND)
                    .entityTypes(List.of(EntityType.PRODUCTIVITY))
                    .dateFrom(now.truncatedTo(ChronoUnit.HOURS).minusHours(1))
                    .dateTo(now.truncatedTo(ChronoUnit.HOURS))
                    .processName(processes.stream().map(ProcessName::from).collect(toList()))
                    .entityFilters(Map.of(
                            EntityType.PRODUCTIVITY, Map.of(
                                    EntityFilters.ABILITY_LEVEL.toJson(),
                                    List.of(String.valueOf(1))
                            )
                    ))
                    .build());
        } catch (Exception exception) {
            return Map.of(EntityType.PRODUCTIVITY, Collections.emptyList());
        }
    }

    private Integer filterProductivity(final Map<EntityType, List<Entity>> staffingForecast,
                                            final String process) {
        final OptionalDouble productivity = staffingForecast.get(EntityType.PRODUCTIVITY).stream()
                .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
                .mapToInt(Entity::getValue)
                .average();

        return productivity.isPresent() ? (int)productivity.getAsDouble() : null;
    }

}
