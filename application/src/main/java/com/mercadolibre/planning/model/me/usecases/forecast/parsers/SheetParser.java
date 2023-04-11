package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;

public interface SheetParser {

  String WEEK_FORMAT_REGEX = "^\\d{1,2}-\\d{4}$";

  String name();

  ForecastSheetDto parse(String warehouseId, MeliSheet sheet, LogisticCenterConfiguration config);
}
