package com.mercadolibre.planning.model.me.usecases.throughput.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Value
public class GetThroughputResult {
    Map<ProcessName, Map<ZonedDateTime, Integer>> throughputByProcess;

    public static GetThroughputResult emptyThroughput() {
        return new GetThroughputResult(emptyMap());
    }

    public Map<ZonedDateTime, Integer> getOrDefault(ProcessName process,
                                                    Map<ZonedDateTime, Integer> other) {
        return throughputByProcess.getOrDefault(process, other);
    }

    public Map<ZonedDateTime, Integer> get(ProcessName process) {
        return throughputByProcess.get(process);
    }
}
