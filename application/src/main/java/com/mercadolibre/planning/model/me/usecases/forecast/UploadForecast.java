package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import lombok.AllArgsConstructor;

import javax.inject.Named;

import java.util.List;

@Named
@AllArgsConstructor
public class UploadForecast {

    private final CreateForecast createForecast;
    private final List<ParseForecastFromFile> parsers;

    public ForecastCreationResponse upload(final String warehouseId,
                                           final Workflow workflow,
                                           final byte[] file,
                                           final Long callerId) {

        final ParseForecastFromFile forecastParser = parsers.stream()
                .filter(parser -> parser.getWorkflow().equals(workflow))
                .findFirst()
                .orElseThrow(() -> new ForecastParsingException(
                        String.format("invalid workflow: %s", workflow)
                ));

        final Forecast forecast = forecastParser.execute(
                new FileUploadDto(warehouseId, workflow, file, callerId)
        );

        return createForecast.execute(new ForecastDto(workflow, forecast));
    }

}
