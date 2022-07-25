package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.entities.workflows.Step;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public enum Process {
  PICKING(Step.TO_PICK),
  PACKING(Step.TO_PACK),
  PACKING_WALL(Step.TO_PACK),
  WAVING(Step.PENDING),
  BATCH_SORTER,
  WALL_IN,
  GLOBAL,
  CHECK_IN,
  SCHEDULED,
  PUT_AWAY,
  RECEIVING;

  public static final Map<Step, Process> PROCESS_BY_STEP = Arrays.stream(values())
      .filter(process -> process.getStep() != null && process.getStep() != Step.TO_PACK)
      .collect(
          toMap(Process::getStep, Function.identity())
      );

  public Step step;

  public static Process getProcessByStep(final Step step) {
    return PROCESS_BY_STEP.get(step);
  }

  public String getName() {
    return name().toLowerCase();
  }

  public Step getStep() {
    return step;
  }

}
