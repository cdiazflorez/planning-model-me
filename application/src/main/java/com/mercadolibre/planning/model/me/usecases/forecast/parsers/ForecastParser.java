package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliDocument;

import java.util.List;

public interface ForecastParser {
    List<ForecastSheetDto> parse(final String warehouseId,
                                 final Workflow workflow,
                                 final MeliDocument document);
}
