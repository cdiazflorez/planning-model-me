package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CycleTimeRequest {

  Set<Workflow> workflows;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  List<ZonedDateTime> slas;

  String timeZone;
}
