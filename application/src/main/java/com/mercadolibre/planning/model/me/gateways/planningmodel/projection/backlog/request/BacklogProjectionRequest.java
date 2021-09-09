package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.BacklogProjectionInput;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;

@Value
@Builder
public class BacklogProjectionRequest {

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
                .processName(List.of(WAVING, PICKING, PACKING, PACKING_WALL))
                .dateFrom(input.getDateFrom())
                .dateTo(getNextHour(input.getDateTo()))
                .currentBacklog(currentBacklogs)
                .applyDeviation(true)
                .build();
    }
}
