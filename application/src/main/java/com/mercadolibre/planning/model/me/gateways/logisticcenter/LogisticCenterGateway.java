package com.mercadolibre.planning.model.me.gateways.logisticcenter;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;

public interface LogisticCenterGateway {

    LogisticCenterConfiguration getConfiguration(final String warehouseId);
}
