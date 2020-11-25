package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class SimulationRequest {
    private Workflow workflow;
    private String warehouseId;
    private List<ProcessName> processName;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private List<QuantityByDate> backlog;
    private List<Simulation> simulations;
    private long userId;
}
