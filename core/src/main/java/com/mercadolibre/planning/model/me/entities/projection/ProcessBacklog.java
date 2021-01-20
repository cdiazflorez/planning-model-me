package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class ProcessBacklog {
    String process;
    @Setter
    int quantity;
    String area;
}
