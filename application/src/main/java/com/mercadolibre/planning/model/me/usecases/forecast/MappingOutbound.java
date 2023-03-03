package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParserHelper.adaptWeekFormat;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY_PP;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_RATIO;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_BATCH_SORTER_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING_WALL_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PICKING_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_WALL_IN_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.ACTIVE_WORKERS;

import com.mercadolibre.planning.model.me.enums.CodeError;
import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.exception.ForecastWorkersInvalidException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.HeadcountRatio;
import com.mercadolibre.planning.model.me.utils.Format;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class MappingOutbound {
  private static final int DECIMAL_QUANTITY = 2;

  private static final String WAREHOUSE_ID = "warehouse_id";

  private static final String INVALID_DATES_ERROR_MESSAGE =
      "`Reps` sheet dates must match `PP - Staffing` sheet dates";

  private MappingOutbound() {}

  static List<HeadcountProductivity> buildHeadcountProductivity(
      final Map<ForecastColumn, Object> parsedValues) {
    if (!parsedValues.containsKey(HEADCOUNT_PRODUCTIVITY_PP)) {
      return (List<HeadcountProductivity>) parsedValues.get(HEADCOUNT_PRODUCTIVITY);
    }

    final List<HeadcountProductivity> hcProductivityPP =
        (List<HeadcountProductivity>) parsedValues.get(HEADCOUNT_PRODUCTIVITY_PP);
    final List<HeadcountProductivity> hcProductivity =
        (List<HeadcountProductivity>) parsedValues.get(HEADCOUNT_PRODUCTIVITY);

    final List<HeadcountProductivity> headcountProductivities = new ArrayList<>(hcProductivity);

    headcountProductivities.addAll(hcProductivityPP);

    return headcountProductivities;
  }

  static List<ProcessingDistribution> buildProcessingDistribution(
      final Map<ForecastColumn, Object> parsedValues) {
    final List<ProcessingDistribution> processingDistribution =
        (List<ProcessingDistribution>) parsedValues.get(PROCESSING_DISTRIBUTION);

    validateActiveAndPresentWorkersColumns(processingDistribution);

    if (!parsedValues.containsKey(HEADCOUNT_RATIO)) {
      return processingDistribution;
    }

    final List<HeadcountRatio> ratiosPP = (List<HeadcountRatio>) parsedValues.get(HEADCOUNT_RATIO);

    final var pickingGlobalHeadcount =
        processingDistribution.stream()
            .filter(
                processing ->
                    PICKING.getName().equals(processing.getProcessName())
                        && processing.getType().equals(EFFECTIVE_WORKERS.toString()))
            .findFirst()
            .orElseThrow();

    checkRatioDates(ratiosPP, pickingGlobalHeadcount);

    final var ratioByProcessPathAndDate = buildRatioByProcessPathAndDate(ratiosPP);

    final var pdPickingRatio =
        ratioByProcessPathAndDate.entrySet().stream()
            .map(
                entry ->
                    new ProcessingDistribution(
                        pickingGlobalHeadcount.getType(),
                        pickingGlobalHeadcount.getQuantityMetricUnit(),
                        pickingGlobalHeadcount.getProcessName(),
                        entry.getKey(),
                        buildDataWithRatio(entry.getValue(), pickingGlobalHeadcount.getData())))
            .collect(Collectors.toList());

    var newProcessingDistribution = new ArrayList<>(processingDistribution);

    newProcessingDistribution.addAll(pdPickingRatio);

    return newProcessingDistribution;
  }

  private static void checkRatioDates(
      final List<HeadcountRatio> ratiosPP, final ProcessingDistribution pickingHeadcount) {
    final var somePpRatio = ratiosPP.stream().findAny().map(HeadcountRatio::getData);

    if (somePpRatio.isEmpty()) {
      return;
    }

    final var ppRatioDates =
        somePpRatio.get().stream()
            .map(HeadcountRatio.HeadcountRatioData::getDate)
            .map(ZonedDateTime::toInstant)
            .collect(Collectors.toSet());

    final var pickingDates =
        pickingHeadcount.getData().stream()
            .map(ProcessingDistributionData::getDate)
            .map(ZonedDateTime::toInstant)
            .collect(Collectors.toSet());

    if ((pickingDates.size() != ppRatioDates.size()) || !(pickingDates.containsAll(ppRatioDates))) {
      throw new ForecastParsingException(INVALID_DATES_ERROR_MESSAGE);
    }
  }

  private static List<ProcessingDistributionData> buildDataWithRatio(
      final Map<ZonedDateTime, Double> ratioByDate, final List<ProcessingDistributionData> pdData) {

    return pdData.stream()
        .map(
            data ->
                new ProcessingDistributionData(
                    data.getDate(),
                    Format.decimalTruncate(
                        data.getQuantity() * ratioByDate.get(data.getDate()), DECIMAL_QUANTITY)))
        .collect(Collectors.toList());
  }

  static List<Metadata> buildForecastMetadata(
      final String warehouseId, final Map<ForecastColumn, Object> parsedValues) {

    final String week = String.valueOf(parsedValues.get(WEEK));
    final String version = String.valueOf(parsedValues.get(VERSION));
    final String monoOrder = String.valueOf(parsedValues.get(MONO_ORDER_DISTRIBUTION));
    final String multiOrder = String.valueOf(parsedValues.get(MULTI_ORDER_DISTRIBUTION));
    final String multiBatch = String.valueOf(parsedValues.get(MULTI_BATCH_DISTRIBUTION));
    final String pickingPolyvalence =
        String.valueOf(parsedValues.get(OUTBOUND_PICKING_PRODUCTIVITY));
    final String batchPolyvalence =
        String.valueOf(parsedValues.get(OUTBOUND_BATCH_SORTER_PRODUCTIVITY));
    final String wallInPolivalence =
        String.valueOf(parsedValues.get(OUTBOUND_WALL_IN_PRODUCTIVITY));
    final String packingPolivalence =
        String.valueOf(parsedValues.get(OUTBOUND_PACKING_PRODUCTIVITY));
    final String packingWallPolivalence =
        String.valueOf(parsedValues.get(OUTBOUND_PACKING_WALL_PRODUCTIVITY));

    return List.of(
        new Metadata(WAREHOUSE_ID, warehouseId),
        new Metadata(WEEK.getName(), adaptWeekFormat(week)),
        new Metadata(VERSION.getName(), version),
        new Metadata(MONO_ORDER_DISTRIBUTION.getName(), monoOrder),
        new Metadata(MULTI_ORDER_DISTRIBUTION.getName(), multiOrder),
        new Metadata(MULTI_BATCH_DISTRIBUTION.getName(), multiBatch),
        new Metadata(OUTBOUND_PICKING_PRODUCTIVITY.getName(), pickingPolyvalence),
        new Metadata(OUTBOUND_BATCH_SORTER_PRODUCTIVITY.getName(), batchPolyvalence),
        new Metadata(OUTBOUND_WALL_IN_PRODUCTIVITY.getName(), wallInPolivalence),
        new Metadata(OUTBOUND_PACKING_PRODUCTIVITY.getName(), packingPolivalence),
        new Metadata(OUTBOUND_PACKING_WALL_PRODUCTIVITY.getName(), packingWallPolivalence));
  }

  private static Map<ProcessPath, Map<ZonedDateTime, Double>> buildRatioByProcessPathAndDate(
      final List<HeadcountRatio> ratiosPP) {
    return ratiosPP.stream()
        .collect(
            Collectors.toMap(
                HeadcountRatio::getProcessPath,
                hcRatio ->
                    hcRatio.getData().stream()
                        .collect(
                            Collectors.toMap(
                                HeadcountRatio.HeadcountRatioData::getDate,
                                HeadcountRatio.HeadcountRatioData::getRatio))));
  }

  private static void validateActiveAndPresentWorkersColumns(
      final List<ProcessingDistribution> processingDistribution) {
    final List<String> invalidActiveAndPresentWorkers =
        processingDistribution.stream()
            .filter(
                distribution ->
                    (distribution.getType().equals(ACTIVE_WORKERS.toString())
                            || distribution.getType().equals(EFFECTIVE_WORKERS.toString()))
                        && distribution.getData().stream().anyMatch(qty -> qty.getQuantity() < 0.0))
            .map(workers -> workers.getProcessName() + "-" + workers.getType())
            .collect(Collectors.toList());

    if (!invalidActiveAndPresentWorkers.isEmpty()) {
      throw new ForecastWorkersInvalidException(
          CodeError.SBO001.getMessage() + " - " + invalidActiveAndPresentWorkers,
          CodeError.SBO001.getName());
    }
  }
}
