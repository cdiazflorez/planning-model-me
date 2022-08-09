package com.mercadolibre.planning.model.me.usecases.throughput;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputInput;
import com.mercadolibre.planning.model.me.usecases.throughput.dtos.GetThroughputResult;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
@AllArgsConstructor
public class GetProcessThroughput implements UseCase<GetThroughputInput, GetThroughputResult> {

  private final PlanningModelGateway planningModelGateway;

  @Override
  public GetThroughputResult execute(GetThroughputInput input) {
    final List<MagnitudePhoto> magnitudes = planningModelGateway.searchTrajectories(request(input))
        .get(THROUGHPUT);

    final Map<ProcessName, List<MagnitudePhoto>> throughputTrajectoriesByProcess = magnitudes.stream()
        .collect(Collectors.groupingBy(
            MagnitudePhoto::getProcessName,
            Collectors.toList()
        ));

    return new GetThroughputResult(
        input.getProcesses()
            .stream()
            .collect(Collectors.toMap(
                Function.identity(),
                process -> calcPessimisticThroughputTrajectory(
                    process.graph,
                    throughputTrajectoriesByProcess
                )
            ))
    );
  }

  private SearchTrajectoriesRequest request(GetThroughputInput input) {
    final List<ProcessName> processes = input.getProcesses()
        .stream()
        .flatMap(processName -> processName.graph.flatten())
        .distinct()
        .collect(Collectors.toList());

    return SearchTrajectoriesRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processName(processes)
        .entityTypes(of(THROUGHPUT))
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(SIMULATION)
        .build();
  }

  /**
   * Calculates the pessimistic throughput trajectory of a process based on the throughput
   * trajectories of all the involved sub-processes.
   */
  public Map<ZonedDateTime, Integer> calcPessimisticThroughputTrajectory(
      final ProcessName.Graph graph,
      final Map<ProcessName, List<MagnitudePhoto>> trajectoryBySubprocess
  ) {
    if (graph instanceof ProcessName.Single) {
      var single = (ProcessName.Single) graph;
      return trajectoryBySubprocess.getOrDefault(single.getProcessName(), of()).stream()
          .collect(Collectors.toMap(MagnitudePhoto::getDate, MagnitudePhoto::getValue));

    } else if (graph instanceof ProcessName.Multiple) {
      var multiple = (ProcessName.Multiple) graph;
      return Arrays.stream(multiple.graphs)
          .flatMap(subGraph -> calcPessimisticThroughputTrajectory(
              subGraph,
              trajectoryBySubprocess
          ).entrySet().stream())
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue,
              multiple.layout == ProcessName.Multiple.Layout.parallel
                  ? Integer::sum
                  : Integer::min
          ));
    } else {
      throw new AssertionError("not reachable");
    }
  }
}
