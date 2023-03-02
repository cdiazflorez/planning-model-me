package com.mercadolibre.planning.model.me.usecases.staffing;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.EFFECTIVE_WORKERS_NS;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EntityFilter {
  PRODUCTIVITY_FILTER(
      PRODUCTIVITY,
      Map.of(
          PRODUCTIVITY, Map.of(EntityFilters.ABILITY_LEVEL.toJson(), List.of(String.valueOf(1))))),
  HEADCOUNT_FILTER(
      HEADCOUNT,
      Map.of(
          HEADCOUNT,
          Map.of(
              PROCESSING_TYPE.toJson(),
              List.of(
                      ACTIVE_WORKERS.getName(),
                      ACTIVE_WORKERS_NS.getName(),
                      EFFECTIVE_WORKERS.getName(),
                      EFFECTIVE_WORKERS_NS.getName()
              )
          )
      )
  );

  private final MagnitudeType magnitudeType;

  private final Map<MagnitudeType, Map<String, List<String>>> entity;

  static Map<MagnitudeType, Map<String, List<String>>> getEntityFilter(
      MagnitudeType magnitudeType) {
    return Arrays.stream(EntityFilter.values())
        .filter(value -> value.magnitudeType.equals(magnitudeType))
        .map(filter -> filter.entity)
        .findAny()
        .orElse(Map.of());
  }
}
