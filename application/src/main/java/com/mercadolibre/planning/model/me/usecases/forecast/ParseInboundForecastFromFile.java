package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper.adaptWeekFormat;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WEEK;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.InboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.InboundRepsForecastSheetParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface ParseInboundForecastFromFile {

  static Forecast parse(
      final String warehouseId,
      final MeliDocument document,
      final long userId,
      final LogisticCenterConfiguration config
  ) {
    var parsedValues = ForecastParserHelper.parseSheets(
        document,
        Stream.of(new InboundRepsForecastSheetParser()),
        warehouseId,
        config
    );

    return InboundForecast.builder()
        .metadata(buildForecastMetadata(warehouseId, parsedValues))
        .processingDistributions((List<ProcessingDistribution>)
            parsedValues.get(PROCESSING_DISTRIBUTION))
        .headcountProductivities((List<HeadcountProductivity>)
            parsedValues.get(HEADCOUNT_PRODUCTIVITY))
        .polyvalentProductivities((List<PolyvalentProductivity>)
            parsedValues.get(POLYVALENT_PRODUCTIVITY))
        .backlogLimits((List<BacklogLimit>)
            parsedValues.get(BACKLOG_LIMITS))
        .userID(userId)
        .build();
  }

  private static List<Metadata> buildForecastMetadata(
      final String warehouseId,
      final Map<ForecastColumn, Object> parsedValues
  ) {
    return List.of(
        new Metadata(WAREHOUSE_ID.getName(), warehouseId),
        new Metadata(WEEK.getName(), adaptWeekFormat(String.valueOf(parsedValues.get(WEEK)))),
        new Metadata(INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES.getName(), String.valueOf(parsedValues.get(
            INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES))),
        new Metadata(INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES.getName(), String.valueOf(parsedValues.get(
            INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES))),
        new Metadata(INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES.getName(), String.valueOf(parsedValues.get(
            INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES)))
    );
  }
}
