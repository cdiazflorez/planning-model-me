package com.mercadolibre.planning.model.me.usecases.forecast.upload.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
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

import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToUtc;
import static java.lang.String.format;

public class SpreadsheetUtils {

    private static final char CHAR_LETTER_A = 'A';
    private static final String HOUR_MINUTE_FORMAT_PATTERN = "^([0]?[0-9]|[0-9][0-9]):[0-5][0-9]$";
    private static final String PARSE_ERROR_MESSAGE = "Error while trying to parse "
            + "cell (%s) for sheet: %s";
    
    public static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private static final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.FRANCE);

    public static MeliDocument createMeliDocumentFrom(final byte[] bytes) {
        try {
            return new PoiDocument(bytes);
        } catch (MeliDocument.MeliDocumentException e) {
            throw new ForecastParsingException("Error while trying to create MeliDocument", e);
        }
    }

    public static MeliCell getCellAt(final MeliRow row, final int column) {
        return row.getCellAt(column);
    }

    public static MeliCell getCellAt(final MeliSheet sheet, final int row, final int column) {
        try {
            return sheet.getRowAt(row).getCellAt(column);
        } catch (NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%s)", getCellAddress(row, column)), e
            );
        }
    }

    private static String getCellAddress(final int row, final int column) {
        return new StringBuilder((char) (((int) CHAR_LETTER_A) + row))
                .append("").append(column + 1).toString();
    }

    public static int getIntValueAt(final MeliSheet sheet, final MeliRow row, final int column) {
        MeliCell cell = getCellAt(row, column);;
        try {
            final String value = cell.getValue();

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).intValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                            cell.getAddress(), sheet.getSheetName()), e
            );
        }
    }

    public static int getIntValueAt(final MeliSheet sheet, final int row, final int column) {
        MeliCell cell = getCellAt(sheet, row, column);
        try {
            final String value = cell.getValue();

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).intValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                            cell.getAddress(),
                            sheet.getSheetName()
                    ), e);
        }
    }

    public static long getLongValueAt(final MeliSheet sheet, final MeliRow row, final int column) {
        MeliCell cell = getCellAt(row, column);;
        try {
            final String value = cell.getValue();

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).longValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                    cell.getAddress(), sheet.getSheetName()),
                    e
            );
        }
    }

    public static long getLongValueAt(final MeliSheet sheet, final int row, final int column) {
        MeliCell cell = getCellAt(sheet, row, column);;
        try {
            final String value = cell.getValue();

            return value == null || value.isEmpty()
                    ? 0L
                    : numberFormatter.parse(value).longValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                    cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    public static double getDoubleValueAt(final MeliSheet sheet, final int row, 
            final int column) {
        MeliCell cell = getCellAt(sheet, row, column);;
        try {
            final String value = cell.getValue();

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).doubleValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                    cell.getAddress(), sheet.getSheetName()), e);
        }
    }
    
    public static ZonedDateTime getDateTimeAt(final MeliSheet sheet, final MeliRow row,
            final int column, final ZoneId zoneId) {
        MeliCell cell = getCellAt(row, column);
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
    
    public static ZonedDateTime getDateTimeAt(final MeliSheet sheet,final int row, 
            final int column,
            final ZoneId zoneId) {
        MeliCell cell = getCellAt(sheet,row, column);
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
        

    public static Duration getDurationAt(final MeliSheet sheet, final int row, final int column,
            String durationFormatPattern) {
        MeliCell cell = getCellAt(sheet,row, column);
        try {
            final String value = cell.getValue();
            validatePattern(durationFormatPattern, value);
            String[] durationData = value.split(":");
            StringBuilder parserStringbuilder = new StringBuilder();
            parserStringbuilder.append("PT").append(durationData[0]).append("H")
                    .append(durationData[1]).append("M");
            Duration durationValue = Duration.parse(parserStringbuilder);
            return durationValue;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE,
                    cell.getAddress(), sheet.getSheetName()), e);
        }
    }

    private static void validatePattern(String durationFormatPattern, final String value) {
        if (!Pattern.compile(durationFormatPattern).matcher(value).matches()) {
            throw new IllegalArgumentException();
        }
    }
    
   
    public static int getIntValueAtFromDuration(final MeliSheet sheet, final int row,
            final int column) {
        MeliCell cell = getCellAt(sheet,row, column);
        try {
            final Duration value = getDurationAt(sheet, row, column, HOUR_MINUTE_FORMAT_PATTERN);
            return Long.valueOf(value.toMinutes()).intValue();
        } catch (NullPointerException e) {
            throw new ForecastParsingException(
                    format(PARSE_ERROR_MESSAGE, 
                    Objects.nonNull(cell) ? cell.getAddress() : null, sheet.getSheetName()),
                    e);
        }
    }
}

