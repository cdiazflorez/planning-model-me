package com.mercadolibre.planning.model.me.usecases.throughput.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
public class GetThroughputInput {
  String warehouseId;

  Workflow workflow;

  List<ProcessName> processes;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  Source source;

  List<Simulation> simulations;

  Map<MagnitudeType, Map<String, List<String>>> entityFilters;
}
