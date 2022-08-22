package com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos;

import java.util.List;
import lombok.Value;

@Value
public class SettingsAtWarehouse {

  String warehouseId;

  List<AreaConfiguration> areas;

}
