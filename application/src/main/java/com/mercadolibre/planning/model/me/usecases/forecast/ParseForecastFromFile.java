package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.UseCase;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.createMeliDocumentFrom;

@AllArgsConstructor
public abstract class ParseForecastFromFile implements UseCase<FileUploadDto, Forecast> {

    private final ForecastParser forecastParser;

    @Override
    public Forecast execute(FileUploadDto input) {
        final String warehouseId = input.getWarehouseId();
        final Workflow workflow = input.getWorkflow();
        final MeliDocument document = createMeliDocumentFrom(input.getBytes());

        final List<ForecastSheetDto> parsedSheets =
                forecastParser.parse(warehouseId, workflow, document);

        final Map<ForecastColumn, Object> parsedValues = parsedSheets.stream()
                .map(ForecastSheetDto::getValues)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return createForecastFrom(warehouseId, parsedValues, input.getUserId());
    }

    public abstract Workflow getWorkflow();

    protected abstract Forecast createForecastFrom(final String warehouseId,
                                                   final Map<ForecastColumn, Object> sheets,
                                                   final long userId);

}
