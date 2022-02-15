package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.ParseInboundForecastFromFile;
import com.mercadolibre.planning.model.me.usecases.forecast.ParseOutboundForecastFromFile;
import lombok.RequiredArgsConstructor;

/** Associates each {@link Workflow} to the {@link ForecastParser} needed to parse the forecasts. */
@RequiredArgsConstructor
public enum Target {
    FBM_WMS_INBOUND(ParseInboundForecastFromFile::parse),
    FBM_WMS_OUTBOUND(ParseOutboundForecastFromFile::parse);

    public final ForecastParser forecastParser;

    public static Target from(Workflow workflow) {
        return Target.valueOf(workflow.name());
    }
}
