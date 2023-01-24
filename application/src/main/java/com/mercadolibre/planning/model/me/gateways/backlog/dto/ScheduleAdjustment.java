package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScheduleAdjustment {
  private List<Workflow> workflow;

  private DeviationType type;

  private List<ShipmentType> affectedShipmentTypes;

  private double value;

  private int units;

  private Instant dateFrom;

  private Instant dateTo;
}
