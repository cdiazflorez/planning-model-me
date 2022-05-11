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
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
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
import javax.inject.Named;
import lombok.AllArgsConstructor;

/**
 * This class contains the methods to process the staffing Request from StaffingController, it calls
 * the Staffing gateway to get the values and process them to create the Response.
 */
@Named
@AllArgsConstructor
public class GetStaffing implements UseCase<GetStaffingInput, Staffing> {

  private static final String PACKING_PROCESS = "packing";

  private static final String PACKING_WALL_PROCESS = "packing_wall";

  private static final List<String> EFFECTIVE_PROCESSES =
      List.of(PACKING_PROCESS, PACKING_WALL_PROCESS);

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

      /* TODO: Ajustar la configuración de Workflow cuando planning model api devuelva otros
      workflows */
      final Map<MagnitudeType, List<MagnitudePhoto>> forecastStaffing =
          workflowConfig.isShouldRetrieveProductivity()
              ? getForecastStaffing(logisticCenterId, workflow, processNames, now, PRODUCTIVITY)
              : Map.of(PRODUCTIVITY, Collections.emptyList());

      final Map<MagnitudeType, List<MagnitudePhoto>> plannedStaffing =
          workflowConfig.isShouldRetrieveHeadcount()
              ? getForecastStaffing(logisticCenterId, workflow, processNames, now, HEADCOUNT)
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

  private Integer total(Stream<Integer> values) {
    final List<Integer> validValues = values.filter(Objects::nonNull).collect(toList());

    return validValues.isEmpty() ? null : validValues.stream().mapToInt(i -> i).sum();
  }

  private Double getProductivity(String process, ProcessTotals totals) {
    if (EFFECTIVE_PROCESSES.contains(process)) {
      return totals.getEffProductivity();
    }

    return totals.getNetProductivity();
  }

  private Process toProcess(
      final String process,
      final StaffingProcess processStaffing,
      final Integer targetProductivity,
      final Integer planned) {

    final ProcessTotals totals = processStaffing.getTotals();

    final Double productivity = getProductivity(process, totals);
    final Integer idle = totals.getIdle();
    final Integer working = totals.getWorkingSystemic();
    final Integer nonSystemicWorkers = totals.getWorkingNonSystemic();
    final Integer delta = planned == null ? null : (working + idle) - planned;
    final Double throughput = totals.getThroughput();

    final Integer realProductivity = productivity == null ? null : productivity.intValue();
    final Integer realThroughput = throughput == null ? null : throughput.intValue();

    final List<Area> areas =
        processStaffing.getAreas().stream()
            .map(
                area -> {
                  final Totals areaTotals = area.getTotals();
                  final Double areaProductivity = areaTotals.getProductivity();
                  return new Area(
                      area.getName(),
                      areaProductivity == null ? null : areaProductivity.intValue(),
                      new Worker(areaTotals.getIdle(), areaTotals.getWorkingSystemic()));
                })
            .sorted(Comparator.comparing(Area::getArea, Comparator.naturalOrder()))
            .collect(toList());

    return Process.builder()
        .process(process)
        .netProductivity(realProductivity)
        .workers(new Worker(idle, working, nonSystemicWorkers, planned, delta))
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
              .dateTo(date)
              .processName(processes.stream().map(ProcessName::from).collect(toList()))
              .entityFilters(EntityFilter.getEntityFilter(magnitudeType))
              .build());

    } catch (Exception exception) {
      return Map.of(magnitudeType, Collections.emptyList());
    }
  }

  private Integer filterProductivity(final Map<MagnitudeType, List<MagnitudePhoto>> staffingForecast, final String process) {
    final OptionalDouble productivity =
        staffingForecast.get(PRODUCTIVITY).stream()
            .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
            .mapToInt(MagnitudePhoto::getValue)
            .average();

    return productivity.isPresent() ? (int) productivity.getAsDouble() : null;
  }

  private Integer filterHeadcount(
      final Map<MagnitudeType, List<MagnitudePhoto>> staffingHeadcount, final String process) {
    return staffingHeadcount.get(HEADCOUNT).stream()
        .filter(entity -> entity.getProcessName().equals(ProcessName.from(process)))
        .map(MagnitudePhoto::getValue)
        .findAny()
        .orElse(null);
  }
}
