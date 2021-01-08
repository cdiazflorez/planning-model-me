package com.mercadolibre.planning.model.me.gateways.outboundwave;

import java.time.ZonedDateTime;

public interface OutboundWaveGateway {

    long getUnitsCount(final String warehouseId,
                       final ZonedDateTime dateFrom,
                       final ZonedDateTime dateTo,
                       final String unitGroupType);
}
