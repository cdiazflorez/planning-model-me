package com.mercadolibre.planning.model.me.entities.staffing;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StaffingWorkflow {

  private String workflow;

  private Integer globalNetProductivity;

  private Integer totalWorkers;

  // TODO: retirar atributo cuando la version en FE se encuentra desplegada.
  private Integer totalNonSystemicWorkers;

  private NonSystemicWorkers nonSystemicWorkers;

  private List<Process> processes;
}
