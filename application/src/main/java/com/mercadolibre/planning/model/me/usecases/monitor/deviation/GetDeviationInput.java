package com.mercadolibre.planning.model.me.usecases.monitor.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GetDeviationInput {

    private final String warehouseId;
    private final Workflow workflow;
    private final ZonedDateTime dateTo;
    private final ZonedDateTime dateFrom;
    private final ZonedDateTime currentTime;
}
