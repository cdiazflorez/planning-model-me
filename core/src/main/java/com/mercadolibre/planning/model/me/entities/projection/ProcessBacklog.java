package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProcessBacklog {
    String process;
    int quantity;
}
