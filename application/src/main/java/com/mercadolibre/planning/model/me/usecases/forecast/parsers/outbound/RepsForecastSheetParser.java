package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.BACKLOG_LIMITS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_BATCH_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.MULTI_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_BATCH_SORTER_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PACKING_WALL_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_PICKING_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.OUTBOUND_WALL_IN_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.NON_EXISTENT_COLUMN_IN_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueAtFromDuration;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getLongValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.exception.UnmatchedWarehouseException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.AreaDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.RepsDistributionDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastHeadcountProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProductivityProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.GenerateBacklogLimitUtil;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepsForecastSheetParser implements SheetParser {

  private static final int DEFAULT_ABILITY_LEVEL = 1;

  private static final int POLYVALENT_ABILITY_LEVEL = 2;

  private static final int MONO_ORDER_COLUMN = 5;

  private static final int MULTI_BATCH_COLUMN = 6;

  private static final int MULTI_ORDER_COLUMN = 7;

  private static final int PROCESSING_DISTRIBUTION_STARTING_ROW = 7;

  private static final int HOURS_PER_FORECAST_PERIOD = 168;

  private static final int POLYVALENT_PRODUCTIVITY_STARTING_ROW = 188;

  private static final Map<SheetVersion, Integer> HEADCOUNT_PRODUCTIVITY_COLUMN_OFFSET =
      SheetVersion.mapping(3, 4);

  private static final int WAREHOUSE_ID_ROW = 3;

  @Override
  public String name() {
    return "Reps";
  }

  @Override
  public ForecastSheetDto parse(
      final String warehouseId, final MeliSheet sheet, final LogisticCenterConfiguration config) {
    final String week = getStringValueAt(sheet, 2, 2);
    final SheetVersion version = SheetVersion.getSheetVersion(sheet);
    validateIfWarehouseIdIsCorrect(warehouseId, sheet);
    validateIfWeekIsCorrect(week);

    final RepsDistributionDto repsDistributionDto =
        getProcessingDistribution(config, sheet, version);

    final Map<String, Double> productivityPolyvalenceByProcessName =
        getPolyvalentProductivity(sheet, version).stream()
            .collect(
                Collectors.toMap(
                    PolyvalentProductivity::getProcessName,
                    PolyvalentProductivity::getProductivity));

    return new ForecastSheetDto(
        sheet.getSheetName(),
        Map.ofEntries(
            Map.entry(WEEK, week),
            Map.entry(VERSION, version.getVersion()),
            Map.entry(
                MONO_ORDER_DISTRIBUTION,
                getDoubleValueAt(sheet, WAREHOUSE_ID_ROW, MONO_ORDER_COLUMN)),
            Map.entry(
                MULTI_BATCH_DISTRIBUTION,
                getDoubleValueAt(sheet, WAREHOUSE_ID_ROW, MULTI_BATCH_COLUMN)),
            Map.entry(
                MULTI_ORDER_DISTRIBUTION,
                getDoubleValueAt(sheet, WAREHOUSE_ID_ROW, MULTI_ORDER_COLUMN)),
            Map.entry(
                OUTBOUND_PICKING_PRODUCTIVITY,
                productivityPolyvalenceByProcessName.get(
                    ForecastProductivityProcessName.PICKING.getName())),
            Map.entry(
                OUTBOUND_BATCH_SORTER_PRODUCTIVITY,
                productivityPolyvalenceByProcessName.get(
                    ForecastProductivityProcessName.BATCH_SORTER.getName())),
            Map.entry(
                OUTBOUND_WALL_IN_PRODUCTIVITY,
                productivityPolyvalenceByProcessName.get(
                    ForecastProductivityProcessName.WALL_IN.getName())),
            Map.entry(
                OUTBOUND_PACKING_PRODUCTIVITY,
                productivityPolyvalenceByProcessName.get(
                    ForecastProductivityProcessName.PACKING.getName())),
            Map.entry(
                OUTBOUND_PACKING_WALL_PRODUCTIVITY,
                productivityPolyvalenceByProcessName.get(
                    ForecastProductivityProcessName.PACKING_WALL.getName())),
            Map.entry(PROCESSING_DISTRIBUTION, repsDistributionDto.getProcessingDistributions()),
            Map.entry(HEADCOUNT_DISTRIBUTION, getHeadcountDistribution(sheet)),
            Map.entry(POLYVALENT_PRODUCTIVITY, getPolyvalentProductivity(sheet, version)),
            Map.entry(HEADCOUNT_PRODUCTIVITY, repsDistributionDto.getHeadcountProductivities()),
            Map.entry(
                BACKLOG_LIMITS,
                GenerateBacklogLimitUtil.generateBacklogLimitBody(config, sheet, version))));
  }

  private void validateIfWarehouseIdIsCorrect(String warehouseId, MeliSheet sheet) {
    final String warehouseIdFromSheet = getStringValueAt(sheet, WAREHOUSE_ID_ROW, 2);
    boolean warehouseIdsAreDifferent = !warehouseIdFromSheet.equalsIgnoreCase(warehouseId);
    if (isNullOrEmpty(warehouseIdFromSheet) || warehouseIdsAreDifferent) {
      throw new UnmatchedWarehouseException(warehouseId, warehouseIdFromSheet);
    }
  }

  private void validateIfWeekIsCorrect(final String week) {
    if (!week.matches(WEEK_FORMAT_REGEX)) {
      throw new ForecastParsingException(
          String.format("Week format should be ww-yyyy instead of: %s ", week));
    }
  }

  private RepsDistributionDto getProcessingDistribution(
      final LogisticCenterConfiguration config, final MeliSheet sheet, final SheetVersion version) {
    // Columns
    final List<ProcessingDistribution> processingDistributions =
        ForecastProcessName.stream()
            .filter(forecastProcessName -> forecastProcessName.getStartingColumn(version) != NON_EXISTENT_COLUMN_IN_VERSION)
            .flatMap(
                forecastProcessName ->
                    forecastProcessName.getProcessTypes(version).stream()
                        .map(
                            forecastProcessType ->
                                new ProcessingDistribution(
                                    forecastProcessType.toString(),
                                    forecastProcessType.getMetricUnit().getName(),
                                    forecastProcessName.toString(),
                                    GLOBAL,
                                    new ArrayList<>())))
            .collect(Collectors.toList());

    final List<HeadcountProductivity> headcountProductivities =
        forecastProductivityProcessNameByVersion(version)
            .map(
                processName ->
                    new HeadcountProductivity(
                        GLOBAL,
                        processName.name(),
                        MetricUnit.UNITS_PER_HOUR.getName(),
                        DEFAULT_ABILITY_LEVEL,
                        new ArrayList<>()))
            .collect(Collectors.toList());

    final ZoneId zoneId = config.getZoneId();

    // Parse data rows
    for (int i = 0; i < HOURS_PER_FORECAST_PERIOD; i++) {
      final int rowIndex = PROCESSING_DISTRIBUTION_STARTING_ROW + i;
      final var rowDate = SpreadsheetUtils.getDateTimeAt(sheet, rowIndex, 1, zoneId);

      processingDistributions.forEach(
          processingDistribution -> {
            final int columnIndex = getColumnIndex(processingDistribution, version);
            processingDistribution
                .getData()
                .add(
                    new ProcessingDistributionData(
                        rowDate,
                        getQuantity(sheet, rowIndex, processingDistribution, columnIndex)));
          });

      headcountProductivities.forEach(
          headcountProductivity -> {
            final int columnIndex =
                ForecastProcessName.from(headcountProductivity.getProcessName())
                        .getStartingColumn(version)
                    + HEADCOUNT_PRODUCTIVITY_COLUMN_OFFSET.get(version);
            headcountProductivity
                .getData()
                .add(
                    new HeadcountProductivityData(
                        rowDate, getLongValueAt(sheet, rowIndex, columnIndex)));
          });
    }

    return new RepsDistributionDto(processingDistributions, headcountProductivities);
  }

  private int getQuantity(
      final MeliSheet sheet,
      final int rowIndex,
      ProcessingDistribution processingDistribution,
      final int columnIndex) {

    return isRemainingProcessing(processingDistribution)
        ? getIntValueAtFromDuration(sheet, rowIndex, columnIndex)
        : getIntValueAt(sheet, rowIndex, columnIndex);
  }

  private boolean isRemainingProcessing(ProcessingDistribution processingDistribution) {
    return ForecastProcessType.REMAINING_PROCESSING
            == ForecastProcessType.from(processingDistribution.getType())
        && MetricUnit.MINUTES == MetricUnit.from(processingDistribution.getQuantityMetricUnit());
  }

  private int getColumnIndex(
      final ProcessingDistribution processingDistribution, final SheetVersion version) {
    return ForecastProcessName.from(processingDistribution.getProcessName())
            .getStartingColumn(version)
        + ForecastProcessType.from(processingDistribution.getType()).getColumnOrder(version);
  }

  private List<HeadcountDistribution> getHeadcountDistribution(final MeliSheet sheet) {
    return Arrays.stream(ForecastHeadcountProcessName.values())
        .map(
            headcountProcessName ->
                HeadcountDistribution.builder()
                    .processName(headcountProcessName.getName())
                    .quantityMetricUnit(MetricUnit.PERCENTAGE.getName())
                    .areas(createAreaDistributionFrom(sheet, headcountProcessName))
                    .build())
        .collect(Collectors.toList());
  }

  private List<AreaDistribution> createAreaDistributionFrom(
      final MeliSheet sheet, final ForecastHeadcountProcessName headcountProcessName) {
    return headcountProcessName.getAreas().stream()
        .map(
            area ->
                AreaDistribution.builder()
                    .areaId(area.getName())
                    .quantity(
                        getDoubleValueAt(
                            sheet, headcountProcessName.getRowIndex(), area.getColumnIndex()))
                    .build())
        .collect(Collectors.toList());
  }

  private List<PolyvalentProductivity> getPolyvalentProductivity(
      final MeliSheet sheet, final SheetVersion version) {
    return forecastProductivityProcessNameByVersion(version)
        .map(
            productivityProcess ->
                PolyvalentProductivity.builder()
                    .abilityLevel(POLYVALENT_ABILITY_LEVEL)
                    .processName(productivityProcess.getName())
                    .productivityMetricUnit(MetricUnit.PERCENTAGE.getName())
                    .productivity(
                        getDoubleValueAt(
                            sheet,
                            POLYVALENT_PRODUCTIVITY_STARTING_ROW,
                            productivityProcess.getColumnIndex(version)))
                    .build())
        .collect(Collectors.toList());
  }

  private Stream<ForecastProductivityProcessName> forecastProductivityProcessNameByVersion(
      final SheetVersion version) {
    return Arrays.stream(ForecastProductivityProcessName.values())
        .filter(forecastProdProcessName -> forecastProdProcessName.getColumnIndex(version) != NON_EXISTENT_COLUMN_IN_VERSION);
  }
}
