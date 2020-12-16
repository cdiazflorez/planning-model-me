package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paging {

    private int limit;
    private int offset;
    private int total;

}
