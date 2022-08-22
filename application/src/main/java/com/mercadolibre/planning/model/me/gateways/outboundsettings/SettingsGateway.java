package com.mercadolibre.planning.model.me.gateways.outboundsettings;

import com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos.SettingsAtWarehouse;

/**
 * Get the warehouse configurations.
 */
public interface SettingsGateway {

  SettingsAtWarehouse getPickingSetting(String logisticCenterId);

}
