package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.spreadsheet.MeliDocument;

@FunctionalInterface
public interface ForecastParser {
    Forecast parse(
            String warehouseId,
            MeliDocument document,
            long userId,
            LogisticCenterConfiguration config
    );
}
