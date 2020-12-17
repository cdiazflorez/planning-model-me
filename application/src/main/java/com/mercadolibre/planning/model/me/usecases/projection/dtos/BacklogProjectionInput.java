package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

import java.util.List;

@Value
public class BacklogProjectionInput {

    Workflow workflow;

    String warehouseId;

    List<ProcessName> processName;

    long userId;
}
