package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers;

import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliDocument;

import java.util.List;

public interface ForecastParser {
    List<ForecastSheetDto> parse(final String warehouseId, final MeliDocument document);
}
