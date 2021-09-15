package com.mercadolibre.planning.model.me.usecases.projection.entities;

import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import lombok.Value;

import java.util.List;

@Value
public class ProjectedBacklog {
    private List<BacklogProjectionResponse> projections;
}
