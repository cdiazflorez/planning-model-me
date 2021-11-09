package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class ProjectionRequest {

    private Workflow workflow;
    private String warehouseId;
    private ProjectionType type;
    private List<ProcessName> processName;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private List<Backlog> backlog;
    private long userId;
    private boolean applyDeviation;
    private String timeZone;
}
