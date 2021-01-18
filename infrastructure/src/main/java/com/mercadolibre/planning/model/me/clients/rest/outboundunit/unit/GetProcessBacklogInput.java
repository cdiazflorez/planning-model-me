package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@SuperBuilder
@Data
public class GetProcessBacklogInput {

    private String warehouseId;
    private Workflow workflow;
    private ZonedDateTime dateTo;
    private ZonedDateTime dateFrom;
    private List<Map<String, String>> status;

    public GetProcessBacklogInput from(final GetMonitorInput input,
                                       final List<Map<String, String>> status) {
        return builder().dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .status(status)
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .build();
    }
}
