package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class EntityRequest {

    private Workflow workflow;

    private EntityType entityType;

    private String warehouseId;

    private ZonedDateTime dateFrom;

    private ZonedDateTime dateTo;

    private Source source;

    private List<ProcessName> processName;

    private List<ProcessingType> processingType;

    private List<Simulation> simulations;

}
