package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper.adaptWeekFormat;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper.parseSheets;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_BATCH_SORTER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.OutboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.RepsForecastSheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.SalesDistributionSheetParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ParseOutboundForecastFromFile {
  static final String WAREHOUSE_ID = "warehouse_id";

  public static Forecast parse(
      final String warehouseId,
      final MeliDocument document,
      final long userId,
      final LogisticCenterConfiguration config) {
    final var parsedValues =
        parseSheets(
            document,
            Stream.of(new RepsForecastSheetParser(), new SalesDistributionSheetParser()),
            warehouseId,
            config);

    return OutboundForecast.builder()
        .metadata(buildForecastMetadata(warehouseId, parsedValues))
        .processingDistributions(
            (List<ProcessingDistribution>) parsedValues.get(PROCESSING_DISTRIBUTION))
        .planningDistributions((List<PlanningDistribution>) parsedValues.get(PLANNING_DISTRIBUTION))
        .headcountDistributions(
            (List<HeadcountDistribution>) parsedValues.get(HEADCOUNT_DISTRIBUTION))
        .headcountProductivities(
            (List<HeadcountProductivity>) parsedValues.get(HEADCOUNT_PRODUCTIVITY))
        .polyvalentProductivities(
            (List<PolyvalentProductivity>) parsedValues.get(POLYVALENT_PRODUCTIVITY))
        .backlogLimits((List<BacklogLimit>) parsedValues.get(BACKLOG_LIMITS))
        .userID(userId)
        .build();
  }

  private static List<Metadata> buildForecastMetadata(
      final String warehouseId, final Map<ForecastColumn, Object> parsedValues) {

    final String week = String.valueOf(parsedValues.get(WEEK));
    final String monoOrder = String.valueOf(parsedValues.get(MONO_ORDER_DISTRIBUTION));
    final String multiOrder = String.valueOf(parsedValues.get(MULTI_ORDER_DISTRIBUTION));
    final String multiBatch = String.valueOf(parsedValues.get(MULTI_BATCH_DISTRIBUTION));
    final String pickingPolyvalence = String.valueOf(parsedValues.get(OUTBOUND_PICKING));
    final String batchPolyvalence = String.valueOf(parsedValues.get(OUTBOUND_BATCH_SORTER));
    final String wallInPolivalence = String.valueOf(parsedValues.get(OUTBOUND_WALL_IN));
    final String packingPolivalence = String.valueOf(parsedValues.get(OUTBOUND_PACKING));
    final String packingWallPolivalence = String.valueOf(parsedValues.get(OUTBOUND_PACKING_WALL));

    return List.of(
        new Metadata(WAREHOUSE_ID, warehouseId),
        new Metadata(WEEK.getName(), adaptWeekFormat(week)),
        new Metadata(MONO_ORDER_DISTRIBUTION.getName(), monoOrder),
        new Metadata(MULTI_ORDER_DISTRIBUTION.getName(), multiOrder),
        new Metadata(MULTI_BATCH_DISTRIBUTION.getName(), multiBatch),
        new Metadata(OUTBOUND_PICKING.getName(), pickingPolyvalence),
        new Metadata(OUTBOUND_BATCH_SORTER.getName(), batchPolyvalence),
        new Metadata(OUTBOUND_WALL_IN.getName(), wallInPolivalence),
        new Metadata(OUTBOUND_PACKING.getName(), packingPolivalence),
        new Metadata(OUTBOUND_PACKING_WALL.getName(), packingWallPolivalence));
  }
}
