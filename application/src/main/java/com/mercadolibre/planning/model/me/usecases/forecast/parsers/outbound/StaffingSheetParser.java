package com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY_PP;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastColumnName.HEADCOUNT_RATIO;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeCellValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleCellValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntCellValueAt;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.BasicForecastStaffingColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.BasicForecastStaffingRatioColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ExtendedForecastStaffingProductivityColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ExtendedForecastStaffingRatioColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastStaffingColumnName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.HeadcountRatio;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.StaffingRow;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StaffingSheetParser implements SheetParser {

  private static final String TARGET_SHEET = "PP - Staffing";

  private static final int FIRST_ROW = 2;

  private static final int LAST_ROW = 169;

  private static final int DATE_COLUMN = 0;

  private static final String DELIMITER = ".\n";

  private static final int DEFAULT_ABILITY_LEVEL = 1;

  private static final double RATIO_LIMIT_UP = 1.01;

  private static final double RATIO_LIMIT_DOWN = 0.99;

  private static final String RATIO_OUT_OF_RANGE_ERROR_MESSAGE = "Ratio out range = %s for date = %s, expected ratio from %s to %s";

  private static final String WAREHOUSE_ID = "ARBA01";

  /**
   * Temporary method that will be removed when dynamically parsing process path columns is implemented,
   * @param warehouseId logistics center id to check if its ARBA01 or not
   * @return if its ARBA01, return a BasicForecast if not, a Extended one.
   */
  private static ForecastStaffingColumnName[] getProductivityRowsByWarehouse(final String warehouseId) {
    return WAREHOUSE_ID.equals(warehouseId)
        ? BasicForecastStaffingColumnName.values()
        : ExtendedForecastStaffingProductivityColumnName.values();
  }

  private static ForecastStaffingColumnName[] getRatioRowsByWarehouse(final String warehouseId) {
    return WAREHOUSE_ID.equals(warehouseId)
        ? BasicForecastStaffingRatioColumnName.values()
        : ExtendedForecastStaffingRatioColumnName.values();
  }

  @Override
  public String name() {
    return TARGET_SHEET;
  }

  @Override
  public ForecastSheetDto parse(final String warehouseId, final MeliSheet sheet, final LogisticCenterConfiguration config) {
    final ZoneId zoneId = config.getZoneId();

    final List<StaffingRow> rows = getRows(warehouseId, sheet, zoneId);

    checkForErrors(rows);

    final List<HeadcountRatio> hcRatios = buildHeadcountRatioProductivity(warehouseId, rows);

    validateTotalRatio(hcRatios, zoneId);

    return new ForecastSheetDto(sheet.getSheetName(), Map.of(
        HEADCOUNT_PRODUCTIVITY_PP, buildHeadcountProductivity(warehouseId, rows),
        HEADCOUNT_RATIO, hcRatios
    ));
  }

  private List<StaffingRow> getRows(final String warehouseId, final MeliSheet sheet, final ZoneId zoneId) {
    return IntStream.rangeClosed(FIRST_ROW, LAST_ROW)
        .mapToObj(row -> readRow(warehouseId, sheet, row, zoneId))
        .collect(Collectors.toList());
  }

  private StaffingRow readRow(final String warehouseId, final MeliSheet sheet, final int row, final ZoneId zoneId) {
    return new StaffingRow(
        getDateTimeCellValueAt(sheet, row, DATE_COLUMN, zoneId),
        Arrays.stream(getProductivityRowsByWarehouse(warehouseId))
            .collect(Collectors.toMap(Function.identity(), c -> getIntCellValueAt(sheet, row, c.getColumnIndex()))),
        Arrays.stream(getRatioRowsByWarehouse(warehouseId))
            .collect(Collectors.toMap(Function.identity(), c -> getDoubleCellValueAt(sheet, row, c.getColumnIndex())))
    );
  }

  private void validateTotalRatio(final List<HeadcountRatio> hcRatios, final ZoneId zoneId) {
    final BiFunction<ZonedDateTime, Double, String> messageBuilder = (date, ratio) -> String.format(
        RATIO_OUT_OF_RANGE_ERROR_MESSAGE,
        date.withZoneSameInstant(zoneId).format(ISO_LOCAL_DATE_TIME),
        ratio,
        RATIO_LIMIT_DOWN, RATIO_LIMIT_UP
    );

    final List<String> errors = hcRatios.stream()
        .flatMap(hcRatio -> hcRatio.getData().stream())
        .collect(Collectors.toMap(
                HeadcountRatio.HeadcountRatioData::getDate,
                HeadcountRatio.HeadcountRatioData::getRatio,
                Double::sum
            )
        )
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > RATIO_LIMIT_UP || entry.getValue() < RATIO_LIMIT_DOWN)
        .map(entry -> messageBuilder.apply(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());

    var errorMessages = errors.isEmpty() ? Optional.empty() : Optional.of(String.join(DELIMITER, errors));

    if (errorMessages.isPresent()) {
      final String message = String.join(DELIMITER, errorMessages.get().toString());
      throw new ForecastParsingException(message);
    }
  }

  private List<HeadcountProductivity> buildHeadcountProductivity(final String warehouseId, final List<StaffingRow> rows) {
    return Arrays.stream(getProductivityRowsByWarehouse(warehouseId)).map(
        column ->
            new HeadcountProductivity(
                column.getProcessPath(),
                ProcessName.PICKING.getName(),
                MetricUnit.UNITS_PER_HOUR.getName(),
                DEFAULT_ABILITY_LEVEL,
                getHeadcountProductivityData(rows, column)
            )

    ).collect(Collectors.toList());
  }

  private List<HeadcountProductivityData> getHeadcountProductivityData(
      final List<StaffingRow> rows, final ForecastStaffingColumnName column) {

    return rows.stream().map(row ->
        new HeadcountProductivityData(
            row.getDate().getValue(),
            row.getProductivities().get(column).getValue()
        )
    ).collect(Collectors.toList());

  }

  private List<HeadcountRatio> buildHeadcountRatioProductivity(final String warehouseId, final List<StaffingRow> rows) {
    return Arrays.stream(getRatioRowsByWarehouse(warehouseId)).map(
        column ->
            new HeadcountRatio(
                column.getProcessPath(),
                getHeadcountRatioData(rows, column)
            )
    ).collect(Collectors.toList());
  }

  private List<HeadcountRatio.HeadcountRatioData> getHeadcountRatioData(
      final List<StaffingRow> rows, final ForecastStaffingColumnName column) {

    return rows.stream().map(row ->
        new HeadcountRatio.HeadcountRatioData(
            row.getDate().getValue(),
            row.getRatios().get(column).getValue()
        )
    ).collect(Collectors.toList());
  }

  private void checkForErrors(final List<StaffingRow> rows) {
    final List<String> errors = rows.stream()
        .flatMap(StaffingRow::getInvalidCells)
        .map(CellValue::getError)
        .collect(Collectors.toList());

    final var errorMessages = errors.isEmpty() ? Optional.empty() : Optional.of(String.join(DELIMITER, errors));

    if (errorMessages.isPresent()) {
      final String message = String.join(DELIMITER, errorMessages.get().toString());
      throw new ForecastParsingException(message);
    }
  }

}
