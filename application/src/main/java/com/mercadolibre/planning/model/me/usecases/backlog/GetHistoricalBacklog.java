package com.mercadolibre.planning.model.me.usecases.backlog;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.GetHistoricalBacklogInput;
import com.mercadolibre.planning.model.me.usecases.backlog.dtos.HistoricalBacklog;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.List.of;

@Slf4j
@Named
@AllArgsConstructor
class GetHistoricalBacklog {
    private final BacklogApiGateway backlogApiGateway;

    Map<ProcessName, HistoricalBacklog> execute(
            GetHistoricalBacklogInput input) {

        Map<ProcessName, Map<Integer, Integer>> averages =
                averageBacklogByProcessAndDate(getBacklog(input));

        return input.getProcesses()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        process -> new HistoricalBacklog(
                                averages.getOrDefault(process, emptyMap())
                        ))
                );
    }

    private List<Backlog> getBacklog(GetHistoricalBacklogInput input) {
        return backlogApiGateway.getBacklog(
                BacklogRequest.builder()
                        .warehouseId(input.getWarehouseId())
                        .workflows(input.getWorkflows())
                        .processes(processes(input.getProcesses()))
                        .groupingFields(of("process"))
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build()
        );
    }

    private Map<ProcessName, Map<Integer, Integer>> averageBacklogByProcessAndDate(
            List<Backlog> backlogs) {

        return backlogs.stream()
                .collect(Collectors.groupingBy(
                                this::processNameFromBacklog,
                                Collectors.groupingBy(
                                        b -> DateUtils.minutesFromWeekStart(
                                                b.getDate().truncatedTo(ChronoUnit.HOURS)),
                                        Collectors.collectingAndThen(
                                                Collectors.averagingInt(Backlog::getTotal),
                                                Double::intValue
                                        ))
                        )
                );
    }

    private List<String> processes(List<ProcessName> processNames) {
        return processNames.stream()
                .map(ProcessName::getName)
                .collect(Collectors.toList());
    }

    private ProcessName processNameFromBacklog(Backlog b) {
        final Map<String, String> keys = b.getKeys();
        return ProcessName.from(keys.get("process"));
    }
}
