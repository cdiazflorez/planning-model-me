package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@EqualsAndHashCode
public class SearchEntitiesRequest {

    private Workflow workflow;

    private List<EntityType> entityTypes;

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private Source source;

    private List<ProcessName> processName;

    private List<Simulation> simulations;

    private Map<EntityType, Map<String, List<String>>> entityFilters;
}
