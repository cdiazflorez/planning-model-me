package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers;

import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;

public interface SheetParser {

    String name();

    ForecastSheetDto parse(final String warehouseId, final MeliSheet sheet);
}
