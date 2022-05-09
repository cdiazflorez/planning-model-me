package com.mercadolibre.planning.model.me.usecases.throughput.dtos;

import static java.util.Collections.emptyMap;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class GetThroughputResult {

  Map<ProcessName, Map<ZonedDateTime, Integer>> throughputByProcess;

  public static GetThroughputResult emptyThroughput() {
    return new GetThroughputResult(emptyMap());
  }

  public Map<ZonedDateTime, Integer> getOrDefault(final ProcessName process,
                                                  final Map<ZonedDateTime, Integer> other) {
    return throughputByProcess.getOrDefault(process, other);
  }

  public Map<ZonedDateTime, Integer> get(final ProcessName process) {
    return throughputByProcess.get(process);
  }

  public Optional<Map<Instant, Integer>> find(final ProcessName process) {
    return Optional.ofNullable(throughputByProcess.get(process))
        .map(throughputByHour ->
            throughputByHour.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().toInstant(),
                Map.Entry::getValue
            ))
        );
  }

  public List<MagnitudePhoto> asMagnitudePhotos() {
    return throughputByProcess.entrySet()
        .stream()
        .flatMap(processEntry -> processEntry.getValue()
            .entrySet()
            .stream()
            .map(dateEntry -> MagnitudePhoto.builder()
                .processName(processEntry.getKey())
                .date(dateEntry.getKey())
                .value(dateEntry.getValue())
                .build()
            )
        )
        .collect(Collectors.toList());
  }
}
