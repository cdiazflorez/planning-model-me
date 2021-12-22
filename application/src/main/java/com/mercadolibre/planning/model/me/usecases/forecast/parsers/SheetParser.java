package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;

public interface SheetParser {

    String name();

    Workflow workflow();

    ForecastSheetDto parse(final String warehouseId, final MeliSheet sheet);
}