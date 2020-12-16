package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Value;

import java.util.List;

@Value
public class BacklogProjectionResponse {

    ProcessName processName;

    List<ProjectionValue> values;
}
