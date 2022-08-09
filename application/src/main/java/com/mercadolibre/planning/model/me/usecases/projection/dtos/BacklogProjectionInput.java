package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacklogProjectionInput {

  Workflow workflow;

  String warehouseId;

  List<ProcessName> processName;

  long userId;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  String groupType;

  List<CurrentBacklog> backlogs;

  Map<ProcessName, List<BacklogPhoto>> backlogPhotoByProcess;

  Map<Instant, Double> packingWallRatios;

  boolean hasWall;
}
