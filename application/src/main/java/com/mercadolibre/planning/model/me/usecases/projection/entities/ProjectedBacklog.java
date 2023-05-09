package com.mercadolibre.planning.model.me.usecases.projection.entities;

import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import java.util.List;
import lombok.Value;

@Value
public class ProjectedBacklog {
    private List<BacklogProjectionResponse> projections;
}
