package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.exception.NullValueAtCellException;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.CellValue;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.ErroneousCellValue;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.excel.ValidCellValue;
import com.mercadolibre.spreadsheet.MeliCell;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import com.mercadolibre.spreadsheet.implementations.poi.PoiDocument;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToUtc;
import static java.lang.String.format;

@Slf4j
public final class SpreadsheetUtils {

    public static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    private static final char CHAR_LETTER_A = 'A';
    private static final String HOUR_MINUTE_FORMAT_PATTERN = "^([0]?[0-9]|[0-9][0-9]):[0-5][0-9]$";
    private static final String PARSE_ERROR_MESSAGE = "Error while trying to parse "
            + "cell (%s) for sheet: %s";

    private static final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.FRANCE);

    private SpreadsheetUtils() {
    }

    public static MeliDocument createMeliDocumentFrom(final byte[] bytes) {
        try {
            return new PoiDocument(bytes);
        } catch (MeliDocument.MeliDocumentException e) {
            throw new ForecastParsingException("Error while trying to create MeliDocument", e);
        }
    }

    public static String getStringValueAt(final MeliSheet sheet, final int row, final int column) {
        return getCellAt(sheet, row, column).getValue();
    }

    public static String getStringValueAt(final MeliSheet sheet,
                                          final MeliRow row,
                                          final int column) {
        return getCellAt(sheet, row, column).getValue();
    }

    private static Number extractValue(final MeliSheet sheet,
                                       final MeliCell cell) {
        try {
            final String value = validateNumberValueOrFail(sheet, cell);
            return numberFormatter.parse(value);
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                            cell.getAddress(), sheet.getSheetName()),
                    e
            );
        }
    }

    private static long getLongValueAt(final MeliSheet sheet, final MeliCell cell) {
        try {
            return extractValue(sheet, cell).longValue();
        } catch (EmptyExcelCellException e) {
            return 0L;
        }
    }

    public static long getLongValueAt(final MeliSheet sheet, final MeliRow row, final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        return getLongValueAt(sheet, cell);
    }

    public static long getLongValueAt(final MeliSheet sheet, final int row, final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        return getLongValueAt(sheet, cell);
    }

    private static double getDoubleValueAt(final MeliSheet sheet, final MeliCell cell) {
        try {
            return extractValue(sheet, cell).doubleValue();
        } catch (EmptyExcelCellException e) {
            return 0.0;
        }
    }

    public static double getDoubleValueAt(final MeliSheet sheet, final int row, final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        return getDoubleValueAt(sheet, cell);
    }

    private static int getIntValueAt(final MeliSheet sheet, final MeliCell cell) {
        try {
            return extractValue(sheet, cell).intValue();
        } catch (EmptyExcelCellException e) {
            return 0;
        }
    }

    public static int getIntValueAt(final MeliSheet sheet, final MeliRow row, final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        return getIntValueAt(sheet, cell);
    }

    public static int getIntValueAt(final MeliSheet sheet, final int row, final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        return getIntValueAt(sheet, cell);
    }

    public static CellValue<Integer> getIntCellValueAt(final MeliSheet sheet,
                                                       final int row,
                                                       final int column) {
        try {
            final MeliCell cell = getCellAt(sheet, row, column);
            return new ValidCellValue<>(extractValue(sheet, cell).intValue());
        } catch (ForecastParsingException | EmptyExcelCellException e) {
            return new ErroneousCellValue<>(e.getMessage());
        }
    }

    public static CellValue<Double> getDoubleCellValueAt(final MeliSheet sheet,
                                                       final int row,
                                                       final int column) {
        try {
            final MeliCell cell = getCellAt(sheet, row, column);
            return new ValidCellValue<>(extractValue(sheet, cell).doubleValue());
        } catch (ForecastParsingException | EmptyExcelCellException e) {
            return new ErroneousCellValue<>(e.getMessage());
        }
    }

    public static double getDoubleValueOrFail(final MeliSheet sheet,
                                              final int row,
                                              final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        try {
            if (cell.getValue() == null || cell.getValue().isEmpty()) {
                throw new NullValueAtCellException(getCellAddress(column, row));
            } else {
                String value = cell.getValue().replace(".", ",");
                return numberFormatter.parse(value).doubleValue();
            }

        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    private static String validateNumberValueOrFail(final MeliSheet sheet, final MeliCell cell) {
        if (cell.getValue() == null || cell.getValue().isEmpty()) {
            throw new EmptyExcelCellException(
                    format(PARSE_ERROR_MESSAGE, cell.getAddress(), sheet.getSheetName())
            );
        }

        return cell.getValue().replace(".", ",");
    }

    public static ZonedDateTime getDateTimeAt(final MeliSheet sheet,
                                              final MeliRow row,
                                              final int column,
                                              final ZoneId zoneId) {
        final MeliCell cell = getCellAt(sheet, row, column);
        try {
            final String value = cell.getValue();

            return convertToUtc(ZonedDateTime.parse(
                    value,
                    formatter.withZone(zoneId)));
        } catch (DateTimeParseException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                            cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    public static ZonedDateTime getDateTimeAt(final MeliSheet sheet,
                                              final int row,
                                              final int column,
                                              final ZoneId zoneId) {
        final MeliCell cell = getCellAt(sheet, row, column);
        try {
            final String value = cell.getValue();

            return convertToUtc(ZonedDateTime.parse(
                    value,
                    formatter.withZone(zoneId)));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                            cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    public static CellValue<ZonedDateTime> getDateTimeCellValueAt(final MeliSheet sheet,
                                                                  final int row,
                                                                  final int column,
                                                                  final ZoneId zoneId) {
        try {
            return new ValidCellValue<>(getDateTimeAt(sheet, row, column, zoneId));
        } catch (ForecastParsingException e) {
            return new ErroneousCellValue<>(e.getMessage());
        }
    }

    public static Duration getDurationAt(final MeliSheet sheet,
                                         final int row,
                                         final int column,
                                         final String durationFormatPattern) {
        final MeliCell cell = getCellAt(sheet, row, column);
        try {
            final String value = cell.getValue();
            validatePattern(durationFormatPattern, value);
            return getDurationValue(value);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                            cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    public static int getIntValueAtFromDuration(final MeliSheet sheet,
                                                final int row,
                                                final int column) {
        final MeliCell cell = getCellAt(sheet, row, column);
        try {
            final Duration value = getDurationAt(sheet, row, column, HOUR_MINUTE_FORMAT_PATTERN);
            return (int) value.toMinutes();
        } catch (NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                            Objects.nonNull(cell) ? cell.getAddress() : null, sheet.getSheetName()),
                    e);
        }
    }

    private static void validatePattern(final String durationFormatPattern, final String value) {
        if (!Pattern.compile(durationFormatPattern).matcher(value).matches()) {
            throw new IllegalArgumentException();
        }
    }

    private static Duration getDurationValue(final String value) {
        final String[] durationData = value.split(":");
        final StringBuilder parserStringbuilder = new StringBuilder();
        parserStringbuilder.append("PT").append(durationData[0]).append("H")
                .append(durationData[1]).append("M");
        return Duration.parse(parserStringbuilder);
    }

    public static String getCellAddress(final int column, final int row) {
        final char letter = (char) (CHAR_LETTER_A + column);
        return letter + "" + (row + 1);
    }

    private static MeliCell getCellAt(final MeliSheet sheet, final MeliRow row, final int column) {
        return row.getCellAt(column);
    }

    private static MeliCell getCellAt(final MeliSheet sheet, final int row, final int column) {
        try {
            return sheet.getRowAt(row).getCellAt(column);
        } catch (NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%s)", getCellAddress(column, row)), e
            );
        }
    }

}

