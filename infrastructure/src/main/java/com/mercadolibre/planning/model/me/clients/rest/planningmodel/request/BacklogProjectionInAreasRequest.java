package com.mercadolibre.planning.model.me.clients.rest.planningmodel.request;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class BacklogProjectionInAreasRequest {

  Instant dateFrom;

  Instant dateTo;

  List<ProcessName> processName;

  List<MagnitudePhoto> throughput;

  List<PlanningDistributionResponse> planningUnits;

  List<BacklogQuantityAtSla> currentBacklog;

  List<BacklogAreaDistribution> areaDistributions;
}
