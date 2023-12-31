package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request;

import static com.mercadolibre.planning.model.me.enums.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static java.util.List.of;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacklogProjectionRequest {

  private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
      FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL),
      FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
  );

  Workflow workflow;

  String warehouseId;

  List<ProcessName> processName;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  List<CurrentBacklog> currentBacklog;

  boolean applyDeviation;

  Map<Instant, Double> packingWallRatios;

  public static BacklogProjectionRequest fromInput(final BacklogProjectionInput input,
                                                   final Map<Instant, Double> packingWallRatios) {

    return BacklogProjectionRequest.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .processName(PROCESS_BY_WORKFLOWS.get(input.getWorkflow()))
        .dateFrom(input.getDateFrom())
        .dateTo(getNextHour(input.getDateTo()))
        .currentBacklog(input.getBacklogs())
        .applyDeviation(true)
        .packingWallRatios(packingWallRatios)
        .build();
  }
}
