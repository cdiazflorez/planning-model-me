package com.mercadolibre.planning.model.me.entities.staffing;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Process {

  private String process;

  private Integer netProductivity;

  private Integer targetProductivity;

  private Integer throughput;

  private PlannedWorker workers;

  private List<Area> areas;
}
