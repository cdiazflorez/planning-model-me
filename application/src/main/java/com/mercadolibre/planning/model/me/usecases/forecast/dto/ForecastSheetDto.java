package com.mercadolibre.planning.model.me.usecases.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ForecastSheetDto {
    final String sheetName;
    final Map<ForecastColumn, Object> values;
}
