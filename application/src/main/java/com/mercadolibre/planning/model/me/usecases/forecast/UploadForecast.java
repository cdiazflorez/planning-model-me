package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import lombok.RequiredArgsConstructor;

import javax.inject.Named;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.createMeliDocumentFrom;

@Named
@RequiredArgsConstructor
public class UploadForecast {

    private final CreateForecast createForecast;

    private final LogisticCenterGateway logisticCenterGateway;

    public ForecastCreationResponse upload(final String warehouseId,
                                           final Workflow workflow,
                                           final ForecastParser forecastParser,
                                           final byte[] bytes,
                                           final Long callerId) {

        final MeliDocument document = createMeliDocumentFrom(bytes);

        var config = logisticCenterGateway.getConfiguration(warehouseId);

        final var forecast = forecastParser.parse(warehouseId, document, callerId, config);

        return createForecast.execute(new ForecastDto(workflow, forecast));
    }
}
