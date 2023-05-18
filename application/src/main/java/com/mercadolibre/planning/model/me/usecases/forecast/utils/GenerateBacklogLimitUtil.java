package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WAVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_LOWER_LIMIT_SHIPPING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_UPPER_LIMIT_SHIPPING;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.NON_EXISTENT_COLUMN_IN_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getCellAddress;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntValueOrFail;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimitData;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class GenerateBacklogLimitUtil {

  private static final int COLUMN_DATE = 1;

  private static final int STARTING_ROW = 7;

  private static final int MAX_ROW = 175;

  private static final int MIN_VALUE_FOR_BACKLOG_LIMITS = 0;

  private static final String BUFFER_OUT_OF_LIMITS_ERROR_MESSAGE =
      "No pudimos cargar el forecast. El buffer (%s) para %s debe estar entre %d y %d unidades.";

  private static final String MIXED_OPTIONAL_VALUES_ERROR_MESSAGE =
      "No pudimos cargar el forecast. Si el buffer (%s) es “-1” el resto debe ser igual";

  public static List<BacklogLimit> generateBacklogLimitBody(
      final LogisticCenterConfiguration config, final MeliSheet sheet, final SheetVersion version) {
    final ZoneId zoneId = config.getZoneId();
    return Arrays.stream(BacklogLimitConf.values())
        .filter(
            backlogLimitConf ->
                backlogLimitConf.getColumn(version) != NON_EXISTENT_COLUMN_IN_VERSION)
        .map(
            conf ->
                new BacklogLimit(
                    ProcessPath.GLOBAL,
                    conf.getProcess(),
                    conf.getType(),
                    conf.getType().getMetricUnit(),
                    getBacklogLimitData(sheet, zoneId, conf, version)))
        .collect(toList());
  }

  private static List<BacklogLimitData> getBacklogLimitData(
      final MeliSheet sheet,
      final ZoneId zoneId,
      final BacklogLimitConf conf,
      final SheetVersion version) {

    return sheet.getRowsStartingFrom(STARTING_ROW).stream()
        .filter(row -> row.getIndex() < MAX_ROW)
        .map(row -> createBacklogLimit(sheet, row, zoneId, conf, version))
        .collect(toList());
  }

  private static BacklogLimitData createBacklogLimit(
      final MeliSheet sheet,
      final MeliRow row,
      final ZoneId zoneId,
      final BacklogLimitConf conf,
      final SheetVersion version) {

    final int column = conf.getColumn(version);
    final int rowIndex = row.getIndex();
    final int units = getIntValueOrFail(sheet, rowIndex, column);

    validateRange(units, conf, rowIndex, column);

    if (rowIndex > STARTING_ROW) {
      final int previousUnits = getIntValueOrFail(sheet, (rowIndex - 1), column);
      validateOptional(units, previousUnits, rowIndex, column);
    }

    return BacklogLimitData.builder()
        .date(getDateTimeAt(sheet, rowIndex, COLUMN_DATE, zoneId))
        .quantity(units)
        .build();
  }

  private static void validateRange(
      final int units, final BacklogLimitConf conf, final int row, final int column) {

    if (!(isOptional(units) || conf.isInValidRange(units))) {
      final String cell = getCellAddress(column, row);
      final String process = conf.getProcess().toString();
      throw new ForecastParsingException(
          String.format(
              BUFFER_OUT_OF_LIMITS_ERROR_MESSAGE,
              cell,
              process,
              conf.getMinValue(),
              conf.getMaxValue()));
    }
  }

  private static void validateOptional(
      final int units, final int previousUnits, final int row, final int column) {
    if (isOptional(units) && !isOptional(previousUnits)) {
      throw new ForecastParsingException(
          String.format(MIXED_OPTIONAL_VALUES_ERROR_MESSAGE, getCellAddress(column, row + 1)));

    } else if (isOptional(previousUnits) && !isOptional(units)) {
      throw new ForecastParsingException(
          String.format(MIXED_OPTIONAL_VALUES_ERROR_MESSAGE, getCellAddress(column, row)));
    }
  }

  private static boolean isOptional(final int units) {
    return units == -1;
  }

  @Getter
  @AllArgsConstructor
  private enum BacklogLimitConf {
    WAVING_LOWER_LIMIT(WAVING, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(4, 4), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    WAVING_UPPER_LIMIT(WAVING, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(5, 5), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PICKING_LOWER_LIMIT(PICKING, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(9, 10), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PICKING_UPPER_LIMIT(PICKING, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(10, 11), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PACKING_LOWER_LIMIT(PACKING, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(14, 16), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PACKING_UPPER_LIMIT(PACKING, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(15, 17), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    BATCH_SORTER_LOWER_LIMIT(
        BATCH_SORTER, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(19, 22), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    BATCH_SORTER_UPPER_LIMIT(
        BATCH_SORTER, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(20, 23), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    WALL_IN_LOWER_LIMIT(WALL_IN, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(24, 28), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    WALL_IN_UPPER_LIMIT(WALL_IN, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(25, 29), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PACKING_WALL_LOWER_LIMIT(
        PACKING_WALL, BACKLOG_LOWER_LIMIT, SheetVersion.mapping(29, 34), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    PACKING_WALL_UPPER_LIMIT(
        PACKING_WALL, BACKLOG_UPPER_LIMIT, SheetVersion.mapping(30, 35), MIN_VALUE_FOR_BACKLOG_LIMITS, Integer.MAX_VALUE),
    HU_ASSEMBLY_LOWER_LIMIT(
        HU_ASSEMBLY,
        BACKLOG_LOWER_LIMIT_SHIPPING,
        SheetVersion.mapping(NON_EXISTENT_COLUMN_IN_VERSION, 40),
        MIN_VALUE_FOR_BACKLOG_LIMITS,
        Integer.MAX_VALUE),
    HU_ASSEMBLY_UPPER_LIMIT(
        HU_ASSEMBLY,
        BACKLOG_UPPER_LIMIT_SHIPPING,
        SheetVersion.mapping(NON_EXISTENT_COLUMN_IN_VERSION, 41),
        MIN_VALUE_FOR_BACKLOG_LIMITS,
        Integer.MAX_VALUE),
    SALES_DISPATCH_LOWER_LIMIT(
        SALES_DISPATCH,
        BACKLOG_LOWER_LIMIT_SHIPPING,
        SheetVersion.mapping(NON_EXISTENT_COLUMN_IN_VERSION, 46),
        MIN_VALUE_FOR_BACKLOG_LIMITS,
        Integer.MAX_VALUE),
    SALES_DISPATCH_UPPER_LIMIT(
        SALES_DISPATCH,
        BACKLOG_UPPER_LIMIT_SHIPPING,
        SheetVersion.mapping(NON_EXISTENT_COLUMN_IN_VERSION, 47),
        MIN_VALUE_FOR_BACKLOG_LIMITS,
        Integer.MAX_VALUE);

    final ForecastProcessName process;

    final ForecastProcessType type;

    final Map<SheetVersion, Integer> columnByVersion;

    final int minValue;

    final int maxValue;

    boolean isInValidRange(final int value) {
      return minValue <= value && value <= maxValue;
    }

    public int getColumn(final SheetVersion version) {
      return this.columnByVersion.get(version);
    }
  }
}