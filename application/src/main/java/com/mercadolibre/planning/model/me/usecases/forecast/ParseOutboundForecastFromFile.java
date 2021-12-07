package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.OutboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;

import javax.inject.Named;

import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;

@Named
public final class ParseOutboundForecastFromFile extends ParseForecastFromFile {

    static final String WAREHOUSE_ID = "warehouse_id";

    public ParseOutboundForecastFromFile(ForecastParser forecastParser) {
        super(forecastParser);
    }

    @Override
    public Workflow getWorkflow() {
        return Workflow.FBM_WMS_OUTBOUND;
    }

    @Override
    protected Forecast createForecastFrom(final String warehouseId,
                                          final Map<ForecastColumn, Object> parsedValues,
                                          final long userId) {

        return OutboundForecast.builder()
                .metadata(buildForecastMetadata(warehouseId, parsedValues))
                .processingDistributions((List<ProcessingDistribution>)
                        parsedValues.get(PROCESSING_DISTRIBUTION))
                .planningDistributions((List<PlanningDistribution>)
                        parsedValues.get(PLANNING_DISTRIBUTION))
                .headcountDistributions((List<HeadcountDistribution>)
                        parsedValues.get(HEADCOUNT_DISTRIBUTION))
                .headcountProductivities((List<HeadcountProductivity>)
                        parsedValues.get(HEADCOUNT_PRODUCTIVITY))
                .polyvalentProductivities((List<PolyvalentProductivity>)
                        parsedValues.get(POLYVALENT_PRODUCTIVITY))
                .backlogLimits((List<BacklogLimit>)
                        parsedValues.get(BACKLOG_LIMITS))
                .userID(userId)
                .build();
    }

    private List<Metadata> buildForecastMetadata(
            final String warehouseId,
            final Map<ForecastColumn, Object> parsedValues) {

        final String week = String.valueOf(parsedValues.get(WEEK));
        final String monoOrder = String.valueOf(parsedValues.get(MONO_ORDER_DISTRIBUTION));
        final String multiOrder = String.valueOf(parsedValues.get(MULTI_ORDER_DISTRIBUTION));
        final String multiBatch = String.valueOf(parsedValues.get(MULTI_BATCH_DISTRIBUTION));

        return List.of(
                new Metadata(WAREHOUSE_ID, warehouseId),
                new Metadata(WEEK.getName(), week),
                new Metadata(MONO_ORDER_DISTRIBUTION.getName(), monoOrder),
                new Metadata(MULTI_ORDER_DISTRIBUTION.getName(), multiOrder),
                new Metadata(MULTI_BATCH_DISTRIBUTION.getName(), multiBatch)
        );
    }
}
