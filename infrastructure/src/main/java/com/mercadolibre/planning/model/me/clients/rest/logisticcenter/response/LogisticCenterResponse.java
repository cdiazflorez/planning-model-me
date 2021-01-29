package com.mercadolibre.planning.model.me.clients.rest.logisticcenter.response;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.OutboundOutput;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogisticCenterResponse {

    private String timeZone;
    private OutboundOutput outbound;

}
