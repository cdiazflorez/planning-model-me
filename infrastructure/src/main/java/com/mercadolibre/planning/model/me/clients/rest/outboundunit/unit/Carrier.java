package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carrier {

    private String id;
    private String name;
    private String serviceId;
    private String serviceName;
    private String canalization;
}
