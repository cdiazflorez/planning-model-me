package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import java.util.Arrays;
import java.util.Locale;
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
  CHECK_IN(Step.CHECK_IN),
  SCHEDULED(Step.SCHEDULED),
  PUT_AWAY(Step.PUT_AWAY),
  RECEIVING;

  private static final Map<Step, Process> PROCESS_BY_STEP = Arrays.stream(values())
      .filter(process -> process.getStep() != null && process.getStep() != Step.TO_PACK)
      .collect(
          toMap(Process::getStep, Function.identity())
      );

  private Step step;

  public static Process getProcessByStep(final Step step) {
    return PROCESS_BY_STEP.get(step);
  }

  public static Process from(final ProcessName processName) {
    return Process.valueOf(processName.getName().toUpperCase(Locale.ENGLISH));
  }

  public String getName() {
    return name().toLowerCase();
  }

  public Step getStep() {
    return step;
  }
}
