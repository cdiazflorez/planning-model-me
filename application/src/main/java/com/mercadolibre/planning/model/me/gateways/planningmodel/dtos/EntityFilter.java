package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntityFilter {
  PRODUCTIVITY_FILTER(
      Map.of(
          PRODUCTIVITY, Map.of(EntityFilters.ABILITY_LEVEL.toJson(), List.of(String.valueOf(1))))),
  HEADCOUNT_FILTER(
      Map.of(HEADCOUNT, Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))));

  private Map<MagnitudeType, Map<String, List<String>>> entity;
}
