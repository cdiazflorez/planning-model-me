package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This class represents the adjustment that was made from a deviation detected.
 */
@AllArgsConstructor
@Getter
public class Deviation {

  private Workflow workflow;
  private DeviationType type;
  private Instant dateFrom;
  private Instant dateTo;
  private double value;
  private ShipmentType path;
}
