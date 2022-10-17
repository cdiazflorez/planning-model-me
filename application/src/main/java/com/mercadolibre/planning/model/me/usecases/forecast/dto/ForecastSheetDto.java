package com.mercadolibre.planning.model.me.usecases.forecast.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForecastSheetDto {
  String sheetName;
  Map<ForecastColumn, Object> values;
}
