package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper.parseSheets;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PLANNING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.OutboundForecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.RepsForecastSheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.SalesDistributionSheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.StaffingSheetParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.util.List;
import java.util.stream.Stream;

public final class ParseOutboundForecastFromFile {

  private static final StaffingSheetParser STAFFING_SHEET_PARSER = new StaffingSheetParser();
  private static final RepsForecastSheetParser REPS_FORECAST_SHEET_PARSER =
      new RepsForecastSheetParser();
  private static final SalesDistributionSheetParser SALES_DISTRIBUTION_SHEET_PARSER =
      new SalesDistributionSheetParser();

  private ParseOutboundForecastFromFile() {}

  public static Forecast parse(
      final String warehouseId,
      final MeliDocument document,
      final long userId,
      final LogisticCenterConfiguration config,
      final UploadForecast.FeatureToggles featureToggles) {
    final var parsedValues = parseSheets(document, selectParsers(document), warehouseId, config);

    return OutboundForecast.builder()
        .metadata(MappingOutbound.buildForecastMetadata(warehouseId, parsedValues))
        .processingDistributions(
            MappingOutbound.buildProcessingDistribution(parsedValues))
        .planningDistributions((List<PlanningDistribution>) parsedValues.get(PLANNING_DISTRIBUTION))
        .headcountDistributions(
            (List<HeadcountDistribution>) parsedValues.get(HEADCOUNT_DISTRIBUTION))
        .headcountProductivities(MappingOutbound.buildHeadcountProductivity(parsedValues))
        .polyvalentProductivities(
            (List<PolyvalentProductivity>) parsedValues.get(POLYVALENT_PRODUCTIVITY))
        .backlogLimits((List<BacklogLimit>) parsedValues.get(BACKLOG_LIMITS))
        .userID(userId)
        .build();
  }

  private static Stream<SheetParser> selectParsers(final MeliDocument document) {
    final MeliSheet processPathStaffingSheet =
        document.getSheetByName(STAFFING_SHEET_PARSER.name());

    if (processPathStaffingSheet != null) {
      return Stream.of(
          REPS_FORECAST_SHEET_PARSER, SALES_DISTRIBUTION_SHEET_PARSER, STAFFING_SHEET_PARSER);
    } else {
      return Stream.of(REPS_FORECAST_SHEET_PARSER, SALES_DISTRIBUTION_SHEET_PARSER);
    }
  }
}
