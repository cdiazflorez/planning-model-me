package com.mercadolibre.planning.model.me.usecases.forecast.upload.dto;

import com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ForecastSheetDto {
    final String sheetName;
    final Map<ForecastColumnName, Object> values;
}
