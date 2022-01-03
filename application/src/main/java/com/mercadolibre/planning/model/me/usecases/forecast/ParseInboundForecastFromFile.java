package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.InboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;

import javax.inject.Named;

import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WEEK;

@Named
public class ParseInboundForecastFromFile extends ParseForecastFromFile {

    public ParseInboundForecastFromFile(ForecastParser forecastParser) {
        super(forecastParser);
    }

    @Override
    public Workflow getWorkflow() {
        return Workflow.FBM_WMS_INBOUND;
    }

    @Override
    protected Forecast createForecastFrom(final String warehouseId,
                                          final Map<ForecastColumn, Object> parsedValues,
                                          final long userId) {

        return InboundForecast.builder()
                .metadata(buildForecastMetadata(warehouseId, parsedValues))
                .processingDistributions((List<ProcessingDistribution>)
                        parsedValues.get(PROCESSING_DISTRIBUTION))
                .headcountProductivities((List<HeadcountProductivity>)
                        parsedValues.get(HEADCOUNT_PRODUCTIVITY))
                .polyvalentProductivities((List<PolyvalentProductivity>)
                        parsedValues.get(POLYVALENT_PRODUCTIVITY))
                .userID(userId)
                .build();
    }

    private List<Metadata> buildForecastMetadata(final String warehouseId,
                                                 final Map<ForecastColumn, Object> parsedValues) {
        return List.of(
                new Metadata(WAREHOUSE_ID.getName(), warehouseId),
                new Metadata(WEEK.getName(), adaptWeekFormat(String.valueOf(parsedValues.get(WEEK))))
        );
    }

}
