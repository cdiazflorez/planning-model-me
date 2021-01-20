package com.mercadolibre.planning.model.me.gateways.outboundwave;

import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;

import java.time.ZonedDateTime;

public interface OutboundWaveGateway {

    UnitsResume getUnitsCount(final String warehouseId,
                              final ZonedDateTime dateFrom,
                              final ZonedDateTime dateTo,
                              final String unitGroupType);
}
