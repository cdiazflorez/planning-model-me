package com.mercadolibre.planning.model.me.usecases.throughput.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
@ToString
public class GetThroughputInput {
    private String warehouseId;

    private Workflow workflow;

    private List<ProcessName> processes;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private Source source;

    private List<Simulation> simulations;

    private Map<MagnitudeType, Map<String, List<String>>> entityFilters;
}
