package com.mercadolibre.planning.model.me.usecases.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Area;
import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Worker;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagVarPhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.ProcessTotals;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingProcess;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingWorkflowResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Totals;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static java.util.stream.Collectors.toList;

@Named
@AllArgsConstructor
public class GetStaffing implements UseCase<GetStaffingInput, Staffing> {

    //TODO: Unificar esta config que esta repetida en staffing-api
    private static final Map<String, List<String>> WORKFLOWS = Map.of(
            "fbm-wms-outbound", List.of(
                    "picking", "batch_sorter", "wall_in", "packing", "packing_wall"
            ),
            "fbm-wms-inbound", List.of("receiving", "check_in", "put_away")
    );
    private final PlanningModelGateway planningModelGateway;
    private final StaffingGateway staffingGateway;

    @Override
    public Staffing execute(final GetStaffingInput input) {
        final String logisticCenterId = input.getLogisticCenterId();
        final ZonedDateTime now = getCurrentUtcDateTime();

        final StaffingResponse staffing = staffingGateway.getStaffing(input.getLogisticCenterId());

        return mapMetricsResults(logisticCenterId, now, staffing);
    }

    private Staffing mapMetricsResults(final String logisticCenterId,
                                       final ZonedDateTime now,
                                       final StaffingResponse staffing) {

        final List<StaffingWorkflow> workflows = new ArrayList<>();

        final Map<String, StaffingWorkflowResponse> staffingByWorkflow = staffing.getWorkflows()
                .stream()
                .collect(Collectors.toMap(StaffingWorkflowResponse::getName, Function.identity()));

        WORKFLOWS.forEach((workflow, processNames) -> {
            final Map<MagnitudeType, List<MagVarPhoto>> forecastStaffing;
            //TODO: Eliminar este IF cuando planning model api devuelva otros workflows
            if ("fbm-wms-outbound".equals(workflow)) {
                forecastStaffing = getForecastStaffing(logisticCenterId, processNames, now);
            } else {
                forecastStaffing = Map.of(MagnitudeType.PRODUCTIVITY, Collections.emptyList());
            }

            final StaffingWorkflowResponse staffingWorkflow = staffingByWorkflow.get(workflow);
            final Map<String, StaffingProcess> staffingByProcess = staffingWorkflow.getProcesses()
                    .stream()
                    .collect(Collectors.toMap(
                            StaffingProcess::getName,
                            Function.identity()
                    ));

            final List<Process> processes = processNames.stream()
                    .map(process -> toProcess(
                            process,
                            staffingByProcess.get(process),
                            filterProductivity(forecastStaffing, process))
                    ).collect(toList());

            final Integer totalNonSystemic = staffingWorkflow.getTotals().getWorkingNonSystemic();
            final Integer totalSystemic = staffingWorkflow.getTotals().getWorkingSystemic();
            final Integer totalIdle = staffingWorkflow.getTotals().getIdle();

            final Integer totalWorkers =
                    total(Stream.of(totalIdle, totalSystemic, totalNonSystemic));

            workflows.add(StaffingWorkflow.builder()
                    .workflow(workflow)
                    .processes(processes)
                    .totalWorkers(totalWorkers)
                    .totalNonSystemicWorkers(totalNonSystemic)
                    .build());
        });

        final Integer totalWorkers =
                total(workflows.stream().map(StaffingWorkflow::getTotalWorkers));

        return Staffing.builder()
                .totalWorkers(totalWorkers)
                .workflows(workflows)
                .build();
    }

    private Integer total(Stream<Integer> values) {
        final List<Integer> validValues = values
                .filter(Objects::nonNull)
                .collect(toList());

        return validValues.isEmpty() ? null : validValues.stream().mapToInt(i -> i).sum();
    }

    private Double getProductivity(String process, ProcessTotals totals) {
        if ("packing".equals(process) || "packing_wall".equals(process)) {
            return totals.getEffProductivity();
        }

        return totals.getNetProductivity();
    }

    private Process toProcess(final String process,
                              final StaffingProcess processStaffing,
                              final Integer targetProductivity) {

        final ProcessTotals totals = processStaffing.getTotals();

        final Double productivity = getProductivity(process, totals);
        final Integer idle = totals.getIdle();
        final Integer working = totals.getWorkingSystemic();
        final Double throughput = totals.getThroughput();

        final Integer realProductivity = productivity == null ? null : productivity.intValue();
        final Integer realThroughput = throughput == null ? null : throughput.intValue();

        final List<Area> areas = processStaffing.getAreas()
                .stream()
                .map(area -> {
                    final Totals areaTotals = area.getTotals();
                    final Double areaProductivity = areaTotals.getProductivity();
                    return new Area(
                            area.getName(),
                            areaProductivity == null ? null : areaProductivity.intValue(),
                            new Worker(
                                    areaTotals.getIdle(),
                                    areaTotals.getWorkingSystemic()
                            )
                    );
                })
                .sorted(Comparator.comparing(Area::getArea, Comparator.naturalOrder()))
                .collect(toList());

        return Process.builder()
                .process(process)
                .netProductivity(realProductivity)
                .workers(new Worker(idle, working))
                .areas(areas)
                .throughput(realThroughput)
                .targetProductivity(targetProductivity)
                .build();
    }

    private Map<MagnitudeType, List<MagVarPhoto>> getForecastStaffing(final String logisticCenterId,
                                                                      final List<String> processes,
                                                                      final ZonedDateTime now) {

        try {
            return planningModelGateway.searchTrajectories(SearchTrajectoriesRequest.builder()
                    .warehouseId(logisticCenterId)
                    .workflow(Workflow.FBM_WMS_OUTBOUND)
                    .entityTypes(List.of(MagnitudeType.PRODUCTIVITY))
                    .dateFrom(now.truncatedTo(ChronoUnit.HOURS).minusHours(1))
                    .dateTo(now.truncatedTo(ChronoUnit.HOURS))
                    .processName(processes.stream().map(ProcessName::from).collect(toList()))
                    .entityFilters(Map.of(
                            MagnitudeType.PRODUCTIVITY, Map.of(
                                    EntityFilters.ABILITY_LEVEL.toJson(),
                                    List.of(String.valueOf(1))
                            )
                    ))
                    .build());
        } catch (Exception exception) {
            return Map.of(MagnitudeType.PRODUCTIVITY, Collections.emptyList());
        }
    }

    private Integer filterProductivity(final Map<MagnitudeType, List<MagVarPhoto>> staffingForecast,
                                       final String process) {
        final OptionalDouble productivity = staffingForecast
                .get(MagnitudeType.PRODUCTIVITY).stream()
                .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
                .mapToInt(MagVarPhoto::getValue)
                .average();

        return productivity.isPresent() ? (int) productivity.getAsDouble() : null;
    }

}
