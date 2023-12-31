package com.mercadolibre.planning.model.me.entities.monitor;

import java.time.Instant;
import java.util.List;
import lombok.Value;

/**
 * Remembers the backlog at some instant break down by area.
 */
@Value
public class DetailedBacklogPhoto {
  /**
   * The instant in question.
   */
  private Instant date;

  /**
   * Total reps at the instant in question
   */
  private Headcount headcount;

  /**
   * Desired backlog at the instant in question.
   */
  private UnitMeasure target;

  /**
   * Total backlog at the instant in question.
   */
  private UnitMeasure total;

  /**
   * The backlog at the instant in question break down by area.
   */
  private List<AreaBacklogDetail> areas;
}
