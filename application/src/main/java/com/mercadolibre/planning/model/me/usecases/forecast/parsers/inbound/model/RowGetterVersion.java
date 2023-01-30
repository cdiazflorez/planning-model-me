package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_CHECK_IN_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_PUT_AWAY_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.ACTIVE_RECEIVING;
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
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PRESENT_RECEIVING_NS;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.PUT_AWAY_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.RECEIVING_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ProcessingDistributionColumn.STAGE_IN_TARGET;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Productivity.CHECK_IN;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Productivity.PUT_AWAY;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.Productivity.RECEIVING;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.INITIAL_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion.SECOND_VERSION;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeCellValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleCellValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getIntCellValueAt;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RowGetterVersion {
    INITIAL(INITIAL_VERSION, new RowGetterInitial(), List.of(CHECK_IN, PUT_AWAY)),
    SYSTEMIC_RECEIVING_VERSION(SECOND_VERSION, new RowGetterReceiving(), List.of(RECEIVING, CHECK_IN, PUT_AWAY));

    private static final int DATE_COLUMN = 1;

    private static final String DELIMITER = ".\n";

    private final SheetVersion sheetVersion;

    private final RowGetter rowGetter;

    private final List<Productivity> productivitiesToCalculate;

    public static RowGetterVersion getRowGetterVersion(final SheetVersion sheetVersion) {
        return Arrays.stream(values())
                .filter(v -> v.sheetVersion == sheetVersion)
                .findFirst()
                .orElse(INITIAL);
    }

    private static CellValue<Integer> getIntCellValue(final MeliSheet sheet,
                                                      final int row,
                                                      final ProcessingDistributionColumn column,
                                                      final SheetVersion sheetVersion) {
        return getIntCellValueAt(sheet, row, column.getColumnIdByVersion().get(sheetVersion));
    }

    private static CellValue<Double> getDoubleCellValue(final MeliSheet sheet,
                                                        final int row,
                                                        final ProcessingDistributionColumn column,
                                                        final SheetVersion sheetVersion) {
        return getDoubleCellValueAt(sheet, row, column.getColumnIdByVersion().get(sheetVersion));
    }

    public static class RowGetterInitial implements RowGetter{


        @Override
        public RepsRow readRows(final MeliSheet sheet,
                                final ZoneId zoneId,
                                final int row,
                                final SheetVersion sheetVersion) {
            return RepsRow.builder()
                    .date(getDateTimeCellValueAt(sheet, row, DATE_COLUMN, zoneId))
                    .receivingWorkload(getIntCellValue(sheet, row, RECEIVING_TARGET, sheetVersion))
                    .checkInWorkload(getIntCellValue(sheet, row, CHECK_IN_TARGET, sheetVersion))
                    .putAwayWorkload(getIntCellValue(sheet, row, PUT_AWAY_TARGET, sheetVersion))
                    .stageInWorkload(getIntCellValue(sheet, row, STAGE_IN_TARGET, sheetVersion))
                    .activeNsRepsReceiving(getIntCellValue(sheet, row, ACTIVE_RECEIVING_NS, sheetVersion))
                    .activeRepsCheckIn(getIntCellValue(sheet, row, ACTIVE_CHECK_IN, sheetVersion))
                    .activeNsRepsCheckIn(getIntCellValue(sheet, row, ACTIVE_CHECK_IN_NS, sheetVersion))
                    .activeRepsPutAway(getIntCellValue(sheet, row, ACTIVE_PUT_AWAY, sheetVersion))
                    .activeNsRepsPutAway(getIntCellValue(sheet, row, ACTIVE_PUT_AWAY_NS, sheetVersion))
                    .presentNsRepsReceiving(getIntCellValue(sheet, row, PRESENT_RECEIVING_NS, sheetVersion))
                    .presentRepsCheckIn(getIntCellValue(sheet, row, PRESENT_CHECK_IN, sheetVersion))
                    .presentNsRepsCheckIn(getIntCellValue(sheet, row, PRESENT_CHECK_IN_NS, sheetVersion))
                    .presentRepsPutAway(getIntCellValue(sheet, row, PRESENT_PUT_AWAY, sheetVersion))
                    .presentNsRepsPutAway(getIntCellValue(sheet, row, PRESENT_PUT_AWAY_NS, sheetVersion))
                    .backlogLowerLimitCheckin(getDoubleCellValue(sheet, row, BACKLOG_LOWER_LIMIT_CHECK_IN, sheetVersion))
                    .backlogUpperLimitCheckin(getDoubleCellValue(sheet, row, BACKLOG_UPPER_LIMIT_CHECK_IN, sheetVersion))
                    .backlogLowerLimitPutAway(getDoubleCellValue(sheet, row, BACKLOG_LOWER_LIMIT_PUT_AWAY, sheetVersion))
                    .backlogUpperLimitPutAway(getDoubleCellValue(sheet, row, BACKLOG_UPPER_LIMIT_PUT_AWAY, sheetVersion))
                    .build();
        }

        @Override
        public Optional<String> getMissingRowValues(final List<RepsRow> repsRows) {
            final List<String> errors = repsRows.stream()
                    .flatMap(row -> Stream.of(
                            row.getDate(),
                            row.getReceivingWorkload(),
                            row.getCheckInWorkload(),
                            row.getPutAwayWorkload(),
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
    }

    public static class RowGetterReceiving implements RowGetter {


        @Override
        public RepsRow readRows(final MeliSheet sheet,
                                final ZoneId zoneId,
                                final int row,
                                final SheetVersion sheetVersion) {
            return RepsRow.builder()
                    .date(getDateTimeCellValueAt(sheet, row, DATE_COLUMN, zoneId))
                    .receivingWorkload(getIntCellValue(sheet, row, RECEIVING_TARGET, sheetVersion))
                    .checkInWorkload(getIntCellValue(sheet, row, CHECK_IN_TARGET, sheetVersion))
                    .putAwayWorkload(getIntCellValue(sheet, row, PUT_AWAY_TARGET, sheetVersion))
                    .stageInWorkload(getIntCellValue(sheet, row, STAGE_IN_TARGET, sheetVersion))
                    .activeRepsReceiving(getIntCellValue(sheet, row, ACTIVE_RECEIVING, sheetVersion))
                    .activeNsRepsReceiving(getIntCellValue(sheet, row, ACTIVE_RECEIVING_NS, sheetVersion))
                    .activeRepsCheckIn(getIntCellValue(sheet, row, ACTIVE_CHECK_IN, sheetVersion))
                    .activeNsRepsCheckIn(getIntCellValue(sheet, row, ACTIVE_CHECK_IN_NS, sheetVersion))
                    .activeRepsPutAway(getIntCellValue(sheet, row, ACTIVE_PUT_AWAY, sheetVersion))
                    .activeNsRepsPutAway(getIntCellValue(sheet, row, ACTIVE_PUT_AWAY_NS, sheetVersion))
                    .presentRepsReceiving(getIntCellValue(sheet, row, PRESENT_RECEIVING, sheetVersion))
                    .presentNsRepsReceiving(getIntCellValue(sheet, row, PRESENT_RECEIVING_NS, sheetVersion))
                    .presentRepsCheckIn(getIntCellValue(sheet, row, PRESENT_CHECK_IN, sheetVersion))
                    .presentNsRepsCheckIn(getIntCellValue(sheet, row, PRESENT_CHECK_IN_NS, sheetVersion))
                    .presentRepsPutAway(getIntCellValue(sheet, row, PRESENT_PUT_AWAY, sheetVersion))
                    .presentNsRepsPutAway(getIntCellValue(sheet, row, PRESENT_PUT_AWAY_NS, sheetVersion))
                    .backlogLowerLimitCheckin(getDoubleCellValue(sheet, row, BACKLOG_LOWER_LIMIT_CHECK_IN, sheetVersion))
                    .backlogUpperLimitCheckin(getDoubleCellValue(sheet, row, BACKLOG_UPPER_LIMIT_CHECK_IN, sheetVersion))
                    .backlogLowerLimitPutAway(getDoubleCellValue(sheet, row, BACKLOG_LOWER_LIMIT_PUT_AWAY, sheetVersion))
                    .backlogUpperLimitPutAway(getDoubleCellValue(sheet, row, BACKLOG_UPPER_LIMIT_PUT_AWAY, sheetVersion))
                    .build();
        }

        @Override
        public Optional<String> getMissingRowValues(List<RepsRow> repsRows) {
            final List<String> errors = repsRows.stream()
                    .flatMap(row -> Stream.of(
                            row.getDate(),
                            row.getReceivingWorkload(),
                            row.getCheckInWorkload(),
                            row.getPutAwayWorkload(),
                            row.getStageInWorkload(),
                            row.getActiveRepsReceiving(),
                            row.getActiveRepsReceiving(),
                            row.getActiveNsRepsReceiving(),
                            row.getActiveRepsCheckIn(),
                            row.getActiveNsRepsCheckIn(),
                            row.getActiveRepsPutAway(),
                            row.getActiveNsRepsPutAway(),
                            row.getPresentRepsReceiving(),
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
    }

}
