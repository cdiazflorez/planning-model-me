package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Indicator{

    Integer shipments;
    Integer units;
    Double percentage;


}
