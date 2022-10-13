package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound;

import static com.mercadolibre.planning.model.me.enums.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.HEADCOUNT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.POLYVALENT_PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.WEEK;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.CHECKIN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.PUTAWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName.RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_CHECK_IN_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_PUT_AWAY_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_RECEIVING_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.BACKLOG_LOWER_LIMIT_CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.BACKLOG_LOWER_LIMIT_PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.BACKLOG_UPPER_LIMIT_CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.BACKLOG_UPPER_LIMIT_PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.CHECK_IN_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_CHECK_IN_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_PUT_AWAY_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_RECEIVING_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.RECEIVING_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.STAGE_IN_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeCellValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getStringValueAt;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.HeadcountProductivityData;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PolyvalentProductivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistributionData;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.SheetParser;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastPolyvalenceInboundProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.MetadataCell;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Productivity;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.RepsRow;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class InboundRepsForecastSheetParser implements SheetParser {

  private static final String TARGET_SHEET = "Plan de Staffing";

  private static final int DATE_COLUMN = 1;

  private static final String DELIMITER = ".\n";

  private static final int FIRST_ROW = 7;

  private static final int LAST_ROW = 174;

  private static final int DEFAULT_ABILITY_LEVEL = 1;

  private static final int POLYVALENT_ABILITY_LEVEL = 2;

  private static final int POLYVALENT_PRODUCTIVITY_STARTING_ROW = 180;

  @Override
  public String name() {
    return TARGET_SHEET;
  }

  @Override
  public ForecastSheetDto parse(
      final String requestWarehouseId,
      final MeliSheet sheet,
      final LogisticCenterConfiguration config
  ) {
    final ZoneId zoneId = config.getZoneId();

    final String week = getStringValueAt(
        sheet, MetadataCell.WEEK.getRow(), MetadataCell.WEEK.getColumn());

    final String warehouseId = getStringValueAt(
        sheet, MetadataCell.WAREHOUSE_ID.getRow(), MetadataCell.WAREHOUSE_ID.getColumn());

    final List<RepsRow> rows = getRows(sheet, zoneId);

    checkForErrors(requestWarehouseId, warehouseId, week, rows);

    final Map<String, Double> productivityPolyvalences = getProductivityPolyvalences(sheet).stream()
        .collect(
            Collectors.toMap(
                PolyvalentProductivity::getProcessName,
                PolyvalentProductivity::getProductivity)
        );

    return new ForecastSheetDto(sheet.getSheetName(), Map.of(
        WEEK, week,
        PROCESSING_DISTRIBUTION, buildProcessingDistributions(rows),
        HEADCOUNT_PRODUCTIVITY, buildHeadcountProductivities(rows),
        POLYVALENT_PRODUCTIVITY, Collections.emptyList(),
        INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES, productivityPolyvalences.get(CHECKIN.getName()),
        INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES, productivityPolyvalences.get(PUTAWAY.getName()),
        INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES, productivityPolyvalences.get(RECEIVING.getName())
    ));
  }

  private void checkForErrors(final String requestWarehouseId,
                              final String warehouseId,
                              final String week,
                              final List<RepsRow> rows) {

    final List<String> errorMessages = Stream.of(
        getInvalidWarehouseId(requestWarehouseId, warehouseId),
        getInvalidWeek(week),
        getMissingValues(rows)
    )
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());


    if (!errorMessages.isEmpty()) {
      final String message = String.join(DELIMITER, errorMessages);
      throw new ForecastParsingException(message);
    }
  }

  private Optional<String> getMissingValues(final List<RepsRow> rows) {
    final List<String> errors = rows.stream()
        .flatMap(row -> Stream.of(
            row.getDate(),
            row.getReceivingWorkload(),
            row.getCheckInWorkload(),
            row.getStageInWorkload(),
            row.getActiveNsRepsReceiving(),
            row.getActiveRepsCheckIn(),
            row.getActiveNsRepsCheckIn(),
            row.getActiveRepsPutAway(),
            row.getActiveNsRepsPutAway(),
            row.getPresentNsRepsReceiving(),
            row.getPresentRepsCheckIn(),
            row.getPresentNsRepsCheckIn(),
            row.getPresentRepsPutAway(),
            row.getPresentNsRepsPutAway(),
            row.getBacklogLowerLimitCheckin(),
            row.getBacklogUpperLimitCheckin(),
            row.getBacklogLowerLimitPutAway(),
            row.getBacklogUpperLimitPutAway()
        ))
        .filter(value -> !value.isValid())
        .map(CellValue::getError)
        .collect(Collectors.toList());

    return errors.isEmpty() ? Optional.empty() : Optional.of(String.join(DELIMITER, errors));
  }

  private Optional<String> getInvalidWeek(final String week) {
    return week.matches(WEEK_FORMAT_REGEX)
        ? Optional.empty()
        : Optional.of("week value is malformed or missing");
  }

  private Optional<String> getInvalidWarehouseId(final String expected, final String actual) {
    return expected.equals(actual)
        ? Optional.empty()
        : Optional.of(String.format(
        "Warehouse id %s is different from warehouse id %s from file",
        expected,
        actual
        )
    );
  }

  private List<RepsRow> getRows(final MeliSheet sheet, final ZoneId zoneId) {
    return IntStream.rangeClosed(FIRST_ROW, LAST_ROW)
        .mapToObj(row -> readRow(sheet, row, zoneId))
        .collect(Collectors.toList());
  }

  private RepsRow readRow(final MeliSheet sheet, final int row, final ZoneId zoneId) {
    return RepsRow.builder()
        .date(getDateTimeCellValueAt(sheet, row, DATE_COLUMN, zoneId))
        .receivingWorkload(getIntCellValueAt(sheet, row, RECEIVING_TARGET))
        .checkInWorkload(getIntCellValueAt(sheet, row, CHECK_IN_TARGET))
        .stageInWorkload(getIntCellValueAt(sheet, row, STAGE_IN_TARGET))
        .activeNsRepsReceiving(getIntCellValueAt(sheet, row, ACTIVE_RECEIVING_NS))
        .activeRepsCheckIn(getIntCellValueAt(sheet, row, ACTIVE_CHECK_IN))
        .activeNsRepsCheckIn(getIntCellValueAt(sheet, row, ACTIVE_CHECK_IN_NS))
        .activeRepsPutAway(getIntCellValueAt(sheet, row, ACTIVE_PUT_AWAY))
        .activeNsRepsPutAway(getIntCellValueAt(sheet, row, ACTIVE_PUT_AWAY_NS))
        .presentNsRepsReceiving(getIntCellValueAt(sheet, row, PRESENT_RECEIVING_NS))
        .presentRepsCheckIn(getIntCellValueAt(sheet, row, PRESENT_CHECK_IN))
        .presentNsRepsCheckIn(getIntCellValueAt(sheet, row, PRESENT_CHECK_IN_NS))
        .presentRepsPutAway(getIntCellValueAt(sheet, row, PRESENT_PUT_AWAY))
        .presentNsRepsPutAway(getIntCellValueAt(sheet, row, PRESENT_PUT_AWAY_NS))
        .backlogLowerLimitCheckin(getDoubleCellValueAt(sheet, row, BACKLOG_LOWER_LIMIT_CHECK_IN))
        .backlogUpperLimitCheckin(getDoubleCellValueAt(sheet, row, BACKLOG_UPPER_LIMIT_CHECK_IN))
        .backlogLowerLimitPutAway(getDoubleCellValueAt(sheet, row, BACKLOG_LOWER_LIMIT_PUT_AWAY))
        .backlogUpperLimitPutAway(getDoubleCellValueAt(sheet, row, BACKLOG_UPPER_LIMIT_PUT_AWAY))
        .build();
  }

  private CellValue<Integer> getIntCellValueAt(final MeliSheet sheet,
                                               final int row,
                                               final ProcessingDistributionColumn column) {
    return SpreadsheetUtils.getIntCellValueAt(sheet, row, column.getColumnId());
  }

  private CellValue<Double> getDoubleCellValueAt(final MeliSheet sheet,
                                                 final int row,
                                                 final ProcessingDistributionColumn column) {
    return SpreadsheetUtils.getDoubleCellValueAt(sheet, row, column.getColumnId());
  }

  private List<HeadcountProductivityData> toHeadcountProductivityData(final List<RepsRow> rows,
                                                                      final Productivity column) {

    return rows.stream()
        .map(row -> new HeadcountProductivityData(
            row.getDate().getValue(),
            column.getMapper().apply(row))
        )
        .collect(Collectors.toList());

  }

  private List<HeadcountProductivity> buildHeadcountProductivities(final List<RepsRow> rows) {
    return Arrays.stream(Productivity.values())
        .map(column -> new HeadcountProductivity(
                GLOBAL,
                column.getProcess().getName(),
                UNITS_PER_HOUR.getName(),
                DEFAULT_ABILITY_LEVEL,
                toHeadcountProductivityData(rows, column)
            )
        )
        .collect(Collectors.toList());
  }

  private List<ProcessingDistributionData> toProcessingDistributionData(
      final List<RepsRow> rows,
      final ProcessingDistributionColumn column) {

    return rows.stream()
        .map(row -> new ProcessingDistributionData(
                row.getDate().getValue(),
                column.getMapper().apply(row)
            )
        )
        .collect(Collectors.toList());
  }

  private List<ProcessingDistribution> buildProcessingDistributions(final List<RepsRow> rows) {

    return Arrays.stream(ProcessingDistributionColumn.values())
        .map(column -> new ProcessingDistribution(
                column.getType().name(),
                column.getUnit().getName(),
                column.getProcess().getName(),
                toProcessingDistributionData(rows, column)
            )
        )
        .collect(Collectors.toList());
  }

  private List<PolyvalentProductivity> getProductivityPolyvalences(final MeliSheet sheet) {
    return ForecastPolyvalenceInboundProcessName.stream()
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
                            productivityProcess.getColumnIndex()))
                    .build())
        .collect(Collectors.toList());
  }
}

