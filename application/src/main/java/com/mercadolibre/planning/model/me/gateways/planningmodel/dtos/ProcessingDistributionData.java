package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class ProcessingDistributionData {
    private ZonedDateTime date;
    private int quantity;
}
