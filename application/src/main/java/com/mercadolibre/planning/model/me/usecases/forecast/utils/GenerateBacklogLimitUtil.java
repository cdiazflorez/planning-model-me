package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimit;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.BacklogLimitData;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessName;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastProcessType;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDateTimeAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueAt;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.getDoubleValueOrFail;
import static java.util.stream.Collectors.toList;

public class GenerateBacklogLimitUtil {

    private GenerateBacklogLimitUtil() { }

    private static final int STARTING_ROW = 7;
    private static final int COLUMN_DATE = 1;

    private static final Double MIN_HOURS_WAVING = 0.0;
    private static final Double MIN_HOURS_PICKING = 0.0;
    private static final Double MIN_HOURS_PACKING = 0.0;

    private static final Double MAX_HOURS_WAVING = 24.0;
    private static final Double MAX_HOURS_PICKING = 5.0;
    private static final Double MAX_HOURS_PACKING = 5.0;

    private static final Integer WAVING_LOWER_COLUMN_INDEX = 20;
    private static final Integer PICKING_LOWER_COLUMN_INDEX = 22;
    private static final Integer PACKING_LOWER_COLUMN_INDEX = 24;

    private static final String WAVING_LOWER_COLUMN_NAME = "U";
    private static final String WAVING_UPPER_COLUMN_NAME = "V";
    private static final String PICKING_LOWER_COLUMN_NAME = "W";
    private static final String PICKING_UPPER_COLUMN_NAME = "X";
    private static final String PACKING_LOWER_COLUMN_NAME = "Y";
    private static final String PACKING_UPPER_COLUMN_NAME = "Z";

    private static final Map<Integer, String> INDEX_TO_COLUMN = Map.of(
            WAVING_LOWER_COLUMN_INDEX, WAVING_LOWER_COLUMN_NAME,
            WAVING_LOWER_COLUMN_INDEX + 1, WAVING_UPPER_COLUMN_NAME,
            PICKING_LOWER_COLUMN_INDEX, PICKING_LOWER_COLUMN_NAME,
            PICKING_LOWER_COLUMN_INDEX + 1, PICKING_UPPER_COLUMN_NAME,
            PACKING_LOWER_COLUMN_INDEX, PACKING_LOWER_COLUMN_NAME,
            PACKING_LOWER_COLUMN_INDEX + 1, PACKING_UPPER_COLUMN_NAME);

    public static List<BacklogLimit> generateBacklogLimitBody(
                            final LogisticCenterConfiguration config,
                            final MeliSheet sheet) {

        return getLimits(config, sheet);
    }

    private static List<BacklogLimit> getLimits(final LogisticCenterConfiguration config,
                                                final MeliSheet sheet) {

        List<BacklogLimit> backlogLimits = new ArrayList<>();

        int column = ForecastProcessType.BACKLOG_LOWER_LIMIT.getColumnOrder();

        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_LOWER_LIMIT,
                        ForecastProcessType.BACKLOG_LOWER_LIMIT.getMetricUnit(),
                        ForecastProcessName.WAVING,
                        getBacklogLimitData(config,
                                sheet,
                                column,
                                MIN_HOURS_WAVING,
                                MAX_HOURS_WAVING,
                                ForecastProcessName.WAVING)));

        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_UPPER_LIMIT,
                        ForecastProcessType.BACKLOG_UPPER_LIMIT.getMetricUnit(),
                        ForecastProcessName.WAVING,
                        getBacklogLimitData(config,
                                sheet,
                                ++column,
                                MIN_HOURS_WAVING,
                                MAX_HOURS_WAVING,
                                ForecastProcessName.WAVING)));

        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_LOWER_LIMIT,
                        ForecastProcessType.BACKLOG_LOWER_LIMIT.getMetricUnit(),
                        ForecastProcessName.PICKING,
                        getBacklogLimitData(config,
                                sheet,
                                ++column,
                                MIN_HOURS_PICKING,
                                MAX_HOURS_PICKING,
                                ForecastProcessName.PICKING)));

        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_UPPER_LIMIT,
                        ForecastProcessType.BACKLOG_UPPER_LIMIT.getMetricUnit(),
                        ForecastProcessName.PICKING,
                        getBacklogLimitData(config,
                                sheet,
                                ++column,
                                MIN_HOURS_PICKING,
                                MAX_HOURS_PICKING,
                                ForecastProcessName.PICKING)));


        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_LOWER_LIMIT,
                        ForecastProcessType.BACKLOG_LOWER_LIMIT.getMetricUnit(),
                        ForecastProcessName.PACKING,
                        getBacklogLimitData(config,
                                sheet,
                                ++column,
                                MIN_HOURS_PACKING,
                                MAX_HOURS_PACKING,
                                ForecastProcessName.PACKING)));

        backlogLimits
                .add(new BacklogLimit(ForecastProcessType.BACKLOG_UPPER_LIMIT,
                        ForecastProcessType.BACKLOG_UPPER_LIMIT.getMetricUnit(),
                        ForecastProcessName.PACKING,
                        getBacklogLimitData(config,
                                sheet,
                                ++column,
                                MIN_HOURS_PACKING,
                                MAX_HOURS_PACKING,
                                ForecastProcessName.PACKING)));

        return backlogLimits;
    }

    private static List<BacklogLimitData> getBacklogLimitData(
            final LogisticCenterConfiguration config,
            final MeliSheet sheet,
            final int column,
            final double minHour,
            final Double maxHour,
            final ForecastProcessName forecastProcessName) {

        return sheet.getRowsStartingFrom(STARTING_ROW).stream()
                .filter(x -> x.getIndex() < 175)
                .map((row) -> createBacklogLimit(row,
                        column,
                        sheet,
                        config,
                        minHour,
                        maxHour,
                        forecastProcessName))
                .collect(toList());
    }


    private static BacklogLimitData createBacklogLimit(
            final MeliRow row,
            final int column,
            final MeliSheet sheet,
            final LogisticCenterConfiguration config,
            final Double minHour,
            final Double maxHour,
            final ForecastProcessName forecastProcessName) {

        final int minutes;

        int rowIndex = row.getIndex();
        Double hours = getDoubleValueOrFail(sheet, rowIndex, column);

        validateRange(hours, minHour, maxHour, forecastProcessName, rowIndex, column);

        if (rowIndex > STARTING_ROW) {
            Double previousHours = getDoubleValueAt(sheet, (rowIndex - 1), column);
            validateExceptionOptional(hours, previousHours, rowIndex, column);
        }

        minutes = isOptional(hours) ? (int) (hours * 1) : (int) (hours * 60);

        return BacklogLimitData.builder()
                .date(getDateTimeAt(sheet, row.getIndex(), COLUMN_DATE, config.getZoneId()))
                .quantity(minutes)
                .build();
    }

    private static void validateRange(final Double hours,
                                      final Double minHours,
                                      final Double maxHours,
                                      final ForecastProcessName forecastProcessName,
                                      final int row,
                                      final int column) {

        if (hours < minHours && hours != -1.0 || hours > maxHours) {
            throw new ForecastParsingException(
                    generateExceptionMessage(forecastProcessName, row, column));
        }
    }

    private static void validateExceptionOptional(final Double hours,
                                                  final Double previousHours,
                                                  final int row,
                                                  final int column) {

        final String bufferColumn = INDEX_TO_COLUMN.get(column);

        final String message1 = "No pudimos cargar el forecast. "
                + "Si el buffer (" + bufferColumn;

        final String message2 = ") es “-1” el resto debe ser igual";

        if (isOptional(hours) && !isOptional(previousHours)) {
            throw new ForecastParsingException(
                    message1 + (row + 1) + message2);

        } else if (isOptional(previousHours) && !isOptional(hours)) {
            throw new ForecastParsingException(
                    message1 + row + message2);
        }

    }

    private static String generateExceptionMessage(
            final ForecastProcessName forecastProcessName,
            final int row,
            final int column) {

        final String processName;
        final String messageValidator;

        if (forecastProcessName.toString().equals(ForecastProcessName.WAVING.toString())) {
            processName = "wave";
            messageValidator = " debe estar entre 0 y 24 hr";

        } else  if (forecastProcessName.toString().equals(ForecastProcessName.PICKING.toString())) {
            processName = "pick";
            messageValidator = " debe estar entre 0 y 5 hr";
        } else {
            processName = "pack";
            messageValidator = " debe estar entre 0 y 5 hr";
        }

        final String bufferColumn = INDEX_TO_COLUMN.get(column);

        final String buffer = bufferColumn + (row + 1);

        return "No pudimos cargar el forecast. El buffer ("
                + buffer + ") para Ready to " + processName
                + messageValidator;
    }

    private static boolean isOptional(final Double hours) {
        return  hours == -1.0;
    }
}
