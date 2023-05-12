package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class ProcessingDistributionData {
    ZonedDateTime date;
    double quantity;
}
