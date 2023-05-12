package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffingWorkflow {

  private String workflow;

  private Integer globalNetProductivity;

  private Integer totalWorkers;

  private NonSystemicWorkers nonSystemicWorkers;

  private List<Process> processes;
}
