package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class EntityRequest {

    private Workflow workflow;

    private EntityType entityType;

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private Source source;

    private List<ProcessName> processName;

    private List<ProcessingType> processingType;

}
