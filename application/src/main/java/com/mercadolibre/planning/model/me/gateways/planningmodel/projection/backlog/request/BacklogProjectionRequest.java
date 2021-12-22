package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static java.util.List.of;

@Value
@Builder
public class BacklogProjectionRequest {

    private static final Map<Workflow, List<ProcessName>> PROCESS_BY_WORKFLOWS = Map.of(
            FBM_WMS_OUTBOUND, of(WAVING, PICKING, PACKING, PACKING_WALL),
            FBM_WMS_INBOUND, of(CHECK_IN, PUT_AWAY)
    );

    Workflow workflow;

    String warehouseId;

    List<ProcessName> processName;

    ZonedDateTime dateFrom;

    ZonedDateTime dateTo;

    List<CurrentBacklog> currentBacklog;

    boolean applyDeviation;

    public static BacklogProjectionRequest fromInput(final BacklogProjectionInput input,
                                                     final List<CurrentBacklog> currentBacklogs) {
        return BacklogProjectionRequest.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .processName(PROCESS_BY_WORKFLOWS.get(input.getWorkflow()))
                .dateFrom(input.getDateFrom())
                .dateTo(getNextHour(input.getDateTo()))
                .currentBacklog(currentBacklogs)
                .applyDeviation(true)
                .build();
    }
}
