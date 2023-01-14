package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.PICKING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WALL_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName.WAVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getCellAddress;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueOrFail;
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

  private static final int MINUTES_IN_HOUR = 60;

  private static final int MAX_ROW = 175;

  private static final String BUFFER_OUT_OF_LIMITS_ERROR_MESSAGE =
      "No pudimos cargar el forecast. El buffer (%s) para %s debe estar entre %d y %d hr.";

  private static final String MIXED_OPTIONAL_VALUES_ERROR_MESSAGE =
      "No pudimos cargar el forecast. Si el buffer (%s) es “-1” el resto debe ser igual";

  public static List<BacklogLimit> generateBacklogLimitBody(
      final LogisticCenterConfiguration config, final MeliSheet sheet, final SheetVersion version) {
    final ZoneId zoneId = config.getZoneId();
    return Arrays.stream(BacklogLimitConf.values())
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
    final double hours = getDoubleValueOrFail(sheet, rowIndex, column);

    validateRange(hours, conf, rowIndex, column);

    if (rowIndex > STARTING_ROW) {
      final double previousHours = getDoubleValueAt(sheet, (rowIndex - 1), column);
      validateOptional(hours, previousHours, rowIndex, column);
    }

    final int minutes = isOptional(hours) ? (int) hours : (int) (hours * MINUTES_IN_HOUR);
    return BacklogLimitData.builder()
        .date(getDateTimeAt(sheet, rowIndex, COLUMN_DATE, zoneId))
        .quantity(minutes)
        .build();
  }

  private static void validateRange(
      final double hours, final BacklogLimitConf conf, final int row, final int column) {

    if (!(isOptional(hours) || conf.isInValidRange(hours))) {
      final String cell = getCellAddress(column, row);
      final String process = conf.getProcess().toString();
      throw new ForecastParsingException(
          String.format(
              BUFFER_OUT_OF_LIMITS_ERROR_MESSAGE,
              cell,
              process,
              (int) conf.getMinValue(),
              (int) conf.getMaxValue()));
    }
  }

  private static void validateOptional(
      final double hours, final double previousHours, final int row, final int column) {
    if (isOptional(hours) && !isOptional(previousHours)) {
      throw new ForecastParsingException(
          String.format(MIXED_OPTIONAL_VALUES_ERROR_MESSAGE, getCellAddress(column, row + 1)));

    } else if (isOptional(previousHours) && !isOptional(hours)) {
      throw new ForecastParsingException(
          String.format(MIXED_OPTIONAL_VALUES_ERROR_MESSAGE, getCellAddress(column, row)));
    }
  }

  private static boolean isOptional(final double hours) {
    return hours == -1.0;
  }

  @Getter
  @AllArgsConstructor
  private enum BacklogLimitConf {
    WAVING_LOWER_LIMIT(
        WAVING,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 4, SheetVersion.NON_SYSTEMIC_VERSION_OB, 4),
        0.0,
        24.0),
    WAVING_UPPER_LIMIT(
        WAVING,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 5, SheetVersion.NON_SYSTEMIC_VERSION_OB, 5),
        0.0,
        24.0),
    PICKING_LOWER_LIMIT(
        PICKING,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 9, SheetVersion.NON_SYSTEMIC_VERSION_OB, 10),
        0.0,
        5.0),
    PICKING_UPPER_LIMIT(
        PICKING,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 10, SheetVersion.NON_SYSTEMIC_VERSION_OB, 11),
        0.0,
        5.0),
    PACKING_LOWER_LIMIT(
        PACKING,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 14, SheetVersion.NON_SYSTEMIC_VERSION_OB, 16),
        0.0,
        5.0),
    PACKING_UPPER_LIMIT(
        PACKING,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 15, SheetVersion.NON_SYSTEMIC_VERSION_OB, 17),
        0.0,
        5.0),
    BATCH_SORTER_LOWER_LIMIT(
        BATCH_SORTER,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 19, SheetVersion.NON_SYSTEMIC_VERSION_OB, 22),
        0.0,
        5.0),
    BATCH_SORTER_UPPER_LIMIT(
        BATCH_SORTER,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 20, SheetVersion.NON_SYSTEMIC_VERSION_OB, 23),
        0.0,
        5.0),
    WALL_IN_LOWER_LIMIT(
        WALL_IN,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 24, SheetVersion.NON_SYSTEMIC_VERSION_OB, 28),
        0.0,
        5.0),
    WALL_IN_UPPER_LIMIT(
        WALL_IN,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 25, SheetVersion.NON_SYSTEMIC_VERSION_OB, 29),
        0.0,
        5.0),
    PACKING_WALL_LOWER_LIMIT(
        PACKING_WALL,
        BACKLOG_LOWER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 29, SheetVersion.NON_SYSTEMIC_VERSION_OB, 34),
        0.0,
        5.0),
    PACKING_WALL_UPPER_LIMIT(
        PACKING_WALL,
        BACKLOG_UPPER_LIMIT,
        Map.of(SheetVersion.CURRENT_VERSION, 30, SheetVersion.NON_SYSTEMIC_VERSION_OB, 35),
        0.0,
        5.0);

    final ForecastProcessName process;

    final ForecastProcessType type;

    final Map<SheetVersion, Integer> columnByVersion;

    final double minValue;

    final double maxValue;

    boolean isInValidRange(final Double value) {
      return minValue <= value && value <= maxValue;
    }

    public int getColumn(final SheetVersion version) {
      return this.columnByVersion.get(version);
    }
  }
}
