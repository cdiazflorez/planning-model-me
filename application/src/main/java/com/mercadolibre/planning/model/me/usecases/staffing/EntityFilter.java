package com.mercadolibre.planning.model.me.usecases.staffing;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters.PROCESSING_TYPE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType.ACTIVE_WORKERS;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityFilters;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EntityFilter {
  PRODUCTIVITY_FILTER(
      PRODUCTIVITY,
      Map.of(
          PRODUCTIVITY, Map.of(EntityFilters.ABILITY_LEVEL.toJson(), List.of(String.valueOf(1))))),
  HEADCOUNT_FILTER(
      HEADCOUNT,
      Map.of(HEADCOUNT, Map.of(PROCESSING_TYPE.toJson(), List.of(ACTIVE_WORKERS.getName()))));

  private MagnitudeType magnitudeType;

  private final Map<MagnitudeType, Map<String, List<String>>> entity;

  static Map<MagnitudeType, Map<String, List<String>>> getEntityFilter(
      MagnitudeType magnitudeType) {
    return Arrays.stream(EntityFilter.values())
        .filter(value -> value.getMagnitudeType().equals(magnitudeType))
        .map(EntityFilter::getEntity)
        .findAny()
        .get();
  }
}
