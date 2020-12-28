package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import lombok.Value;

@Value
public class CurrentBacklog {

    ProcessName processName;

    int quantity;
}
