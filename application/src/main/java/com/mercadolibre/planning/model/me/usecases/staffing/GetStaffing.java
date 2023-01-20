package com.mercadolibre.planning.model.me.usecases.staffing;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDateTime;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.entities.staffing.Area;
import com.mercadolibre.planning.model.me.entities.staffing.NonSystemicWorkers;
import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Worker;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.ProcessTotals;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingProcess;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingWorkflowResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Totals;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import lombok.AllArgsConstructor;

/**
 * This class contains the methods to process the staffing Request from StaffingController, it calls
 * the Staffing gateway to get the values and process them to create the Response.
 */
@Named
@AllArgsConstructor
public class GetStaffing implements UseCase<GetStaffingInput, Staffing> {

  private static final String HU_ASSEMBLY_PROCESS = "hu_assembly";

  private static final String SALES_DISPATCH_PROCESS = "sales_dispatch";

  private static final List<String> PROCESSES_WITHOUT_METRICS_FORECAST =
      List.of(HU_ASSEMBLY_PROCESS, SALES_DISPATCH_PROCESS);

  private static final List<String> EFFECTIVE_PROCESSES =
      List.of(ProcessName.PACKING.getName(), ProcessName.PACKING_WALL.getName());

  private final PlanningModelGateway planningModelGateway;

  private final StaffingGateway staffingGateway;

  @Override
  public Staffing execute(final GetStaffingInput input) {

    final StaffingResponse staffing = staffingGateway.getStaffing(input.getLogisticCenterId());

    return mapMetricsResults(input.getLogisticCenterId(), getCurrentUtcDateTime(), staffing);
  }

  private Staffing mapMetricsResults(
      final String logisticCenterId, final ZonedDateTime now, final StaffingResponse staffing) {

    final List<StaffingWorkflow> workflows = new ArrayList<>();

    final Map<String, StaffingWorkflowResponse> staffingByWorkflow =
        staffing.getWorkflows().stream()
            .collect(Collectors.toMap(StaffingWorkflowResponse::getName, Function.identity()));

    for (StaffingWorkflowConfig workflowConfig : StaffingWorkflowConfig.values()) {
      final List<String> processNames = workflowConfig.getProcesses();
      final String workflow = workflowConfig.getName();
      final List<String> processNamesToGetTargetMetrics =
          Workflow.FBM_WMS_OUTBOUND.getName().equals(workflow)
              ? processNames.stream()
                  .filter(process -> !PROCESSES_WITHOUT_METRICS_FORECAST.contains(process))
                  .collect(toList())
              : processNames;

      /* TODO: Ajustar la configuración de Workflow cuando planning model api devuelva otros workflows */
      final Map<MagnitudeType, List<MagnitudePhoto>> forecastStaffing =
          workflowConfig.isShouldRetrieveProductivity()
              ? getForecastStaffing(
                  logisticCenterId, workflow, processNamesToGetTargetMetrics, now, PRODUCTIVITY)
              : Map.of(PRODUCTIVITY, Collections.emptyList());

      final Map<MagnitudeType, List<MagnitudePhoto>> plannedStaffing =
          workflowConfig.isShouldRetrieveHeadcount()
              ? getForecastStaffing(
                  logisticCenterId, workflow, processNamesToGetTargetMetrics, now, HEADCOUNT)
              : Map.of(HEADCOUNT, Collections.emptyList());

      final StaffingWorkflowResponse staffingWorkflow = staffingByWorkflow.get(workflow);

      if (staffingWorkflow != null) {
        final Map<String, StaffingProcess> staffingByProcess =
            staffingWorkflow.getProcesses().stream()
                .collect(Collectors.toMap(StaffingProcess::getName, Function.identity()));

        final List<Process> processes =
            processNames.stream()
                .map(
                    process ->
                        toProcess(
                            process,
                            staffingByProcess.get(process),
                            filterProductivity(forecastStaffing, process),
                            filterHeadcount(plannedStaffing, process)))
                .collect(toList());

        final Integer totalNonSystemic = staffingWorkflow.getTotals().getWorkingNonSystemic();
        final Integer totalSystemic = staffingWorkflow.getTotals().getWorkingSystemic();
        final Integer totalIdle = staffingWorkflow.getTotals().getIdle();

        final Integer totalWorkers = total(Stream.of(totalIdle, totalSystemic, totalNonSystemic));
        final int totalNonSystemicProcess =
            processes.stream()
                .map(value -> value.getWorkers().getNonSystemic())
                .filter(Objects::nonNull)
                .mapToInt(i -> i)
                .sum();
        final Integer totalCross =
            (totalNonSystemic == null ? 0 : totalNonSystemic) - totalNonSystemicProcess;

        workflows.add(
            StaffingWorkflow.builder()
                .workflow(workflow)
                .processes(processes)
                .totalWorkers(totalWorkers)
                .nonSystemicWorkers(
                    NonSystemicWorkers.builder()
                        .total(totalNonSystemic)
                        .subProcesses(totalNonSystemicProcess)
                        .cross(totalCross)
                        .build())
                .build());
      }
    }

    final Integer totalWorkers = total(workflows.stream().map(StaffingWorkflow::getTotalWorkers));

    return Staffing.builder().totalWorkers(totalWorkers).workflows(workflows).build();
  }

  private Integer total(final Stream<Integer> values) {
    final List<Integer> validValues = values.filter(Objects::nonNull).collect(toList());

    return validValues.isEmpty() ? null : validValues.stream().mapToInt(i -> i).sum();
  }

  private Double getProductivity(final String process, final ProcessTotals totals) {
    if (EFFECTIVE_PROCESSES.contains(process)) {
      return totals.getEffProductivity();
    }

    return totals.getNetProductivity();
  }

  private Integer calculateHeadcountDelta(
      final Integer working, final Integer idle, final Integer planned) {
    return planned == null || working == null || idle == null ? null : (working + idle) - planned;
  }

  private Integer calculateNonSystemicHeadcountDelta(
      final Integer nonSystemicWorkers, final Integer nonSystemicPlanned) {
    return nonSystemicPlanned == null || nonSystemicWorkers == null
        ? null
        : nonSystemicWorkers - nonSystemicPlanned;
  }

  private Process toProcess(
      final String process,
      final StaffingProcess processStaffing,
      final Integer targetProductivity,
      final Map<ProcessingType, Optional<Integer>> plannedHeadcount) {

    final ProcessTotals totals = processStaffing.getTotals();

    final Integer systemicWorkersPlanned =
        plannedHeadcount.get(ProcessingType.ACTIVE_WORKERS).orElse(null);
    final Integer nonSystemicWorkersPlanned =
        plannedHeadcount.get(ProcessingType.TOTAL_WORKERS_NS).orElse(null);

    final Integer idle = totals.getIdle();
    final Integer working = totals.getWorkingSystemic();
    final Integer nonSystemicWorkers = totals.getWorkingNonSystemic();
    final Integer delta = calculateHeadcountDelta(working, idle, systemicWorkersPlanned);
    final Integer nonSystemicDelta =
        calculateNonSystemicHeadcountDelta(nonSystemicWorkers, nonSystemicWorkersPlanned);

    final Double productivity = getProductivity(process, totals);
    final Integer realProductivity = productivity == null ? null : productivity.intValue();
    final Double throughput = totals.getThroughput();
    final Integer realThroughput = throughput == null ? null : throughput.intValue();

    final List<Area> areas =
        Optional.ofNullable(processStaffing.getAreas())
            .map(
                staffingAreas ->
                    staffingAreas.stream()
                        .map(
                            area -> {
                              final Totals areaTotals = area.getTotals();
                              final Double areaProductivity = areaTotals.getProductivity();
                              return new Area(
                                  area.getName(),
                                  areaProductivity == null ? null : areaProductivity.intValue(),
                                  new Worker(
                                      areaTotals.getIdle(), areaTotals.getWorkingSystemic()));
                            })
                        .sorted(Comparator.comparing(Area::getArea))
                        .collect(toList()))
            .orElse(Collections.emptyList());

    return Process.builder()
        .process(process)
        .netProductivity(realProductivity)
        .workers(
            new Worker(
                idle,
                working,
                nonSystemicWorkers,
                systemicWorkersPlanned,
                nonSystemicWorkersPlanned,
                delta,
                nonSystemicDelta))
        .areas(areas)
        .throughput(realThroughput)
        .targetProductivity(targetProductivity)
        .build();
  }

  private Map<MagnitudeType, List<MagnitudePhoto>> getForecastStaffing(
      final String logisticCenterId,
      final String workflow,
      final List<String> processes,
      final ZonedDateTime now,
      final MagnitudeType magnitudeType) {

    final ZonedDateTime date = now.truncatedTo(ChronoUnit.HOURS);

    try {

      return planningModelGateway.searchTrajectories(
          SearchTrajectoriesRequest.builder()
              .warehouseId(logisticCenterId)
              .workflow(Workflow.from(workflow).get())
              .entityTypes(List.of(magnitudeType))
              .dateFrom(magnitudeType.equals(PRODUCTIVITY) ? date.minusHours(1) : date)
              .source(Source.SIMULATION)
              .dateTo(date)
              .processName(processes.stream().map(ProcessName::from).collect(toList()))
              .entityFilters(EntityFilter.getEntityFilter(magnitudeType))
              .build());

    } catch (Exception exception) {
      return Map.of(magnitudeType, Collections.emptyList());
    }
  }

  private Integer filterProductivity(
      final Map<MagnitudeType, List<MagnitudePhoto>> staffingForecast, final String process) {
    final OptionalDouble productivity =
        !PROCESSES_WITHOUT_METRICS_FORECAST.contains(process)
            ? staffingForecast.get(PRODUCTIVITY).stream()
                .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
                .mapToInt(MagnitudePhoto::getValue)
                .average()
            : OptionalDouble.empty();

    return productivity.isPresent() ? (int) productivity.getAsDouble() : null;
  }

  private Map<ProcessingType, Optional<Integer>> filterHeadcount(
      final Map<MagnitudeType, List<MagnitudePhoto>> plannedStaffing, final String process) {

    final List<MagnitudePhoto> staffingHeadcount =
        !PROCESSES_WITHOUT_METRICS_FORECAST.contains(process)
            ? plannedStaffing.get(HEADCOUNT).stream()
                .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
                .collect(toList())
            : Collections.emptyList();

    return Map.of(
        ProcessingType.ACTIVE_WORKERS,
            getHeadcountByProcessingType(staffingHeadcount, ProcessingType.ACTIVE_WORKERS),
        ProcessingType.TOTAL_WORKERS_NS,
            getHeadcountByProcessingType(staffingHeadcount, ProcessingType.TOTAL_WORKERS_NS));
  }

  private Optional<Integer> getHeadcountByProcessingType(
      final List<MagnitudePhoto> staffingHeadcount, final ProcessingType processingType) {
    Optional<MagnitudePhoto> magnitudePhoto =
        staffingHeadcount.stream()
            .filter(
                entity ->
                    entity.getSource().equals(Source.SIMULATION)
                        && entity.getType().equals(processingType))
            .findAny();

    if (magnitudePhoto.isEmpty()) {
      magnitudePhoto =
          staffingHeadcount.stream()
              .filter(
                  entity ->
                      entity.getSource().equals(Source.FORECAST)
                          && entity.getType().equals(processingType))
              .findAny();
    }

    return magnitudePhoto.map(MagnitudePhoto::getValue);
  }
}
