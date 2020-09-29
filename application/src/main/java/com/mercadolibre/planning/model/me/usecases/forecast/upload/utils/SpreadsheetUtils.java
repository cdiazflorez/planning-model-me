package com.mercadolibre.planning.model.me.usecases.forecast.upload.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import com.mercadolibre.spreadsheet.implementations.poi.PoiDocument;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.lang.String.format;

public class SpreadsheetUtils {

    public static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy H:mm");
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:00");
    private static final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.FRANCE);

    public static MeliDocument createMeliDocumentFrom(final byte[] bytes) {
        try {
            return new PoiDocument(bytes);
        } catch (MeliDocument.MeliDocumentException e) {
            throw new ForecastParsingException("Error while trying to create MeliDocument", e);
        }
    }

    public static String getValueAt(final MeliRow row, final int column) {
        return row.getCellAt(column).getValue();
    }

    public static String getValueAt(final MeliSheet sheet, final int row, final int column) {
        return sheet.getRowAt(row).getCellAt(column).getValue();
    }

    public static int getIntValueAt(final MeliRow row, final int column) {
        try {
            return numberFormatter.parse(getValueAt(row, column)).intValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%d, %d)", row.getIndex(), column),
                    e
            );
        }
    }

    public static int getIntValueAt(final MeliSheet sheet, final int row, final int column) {
        try {
            final String value = getValueAt(sheet, row, column);

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).intValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%d, %d)" + " for sheet: %s",
                            row,
                            column,
                            sheet.getSheetName()
                    ), e);
        }
    }

    public static long getLongValueAt(final MeliRow row, final int column) {
        try {
            final String value = getValueAt(row, column);

            return value == null || value.isEmpty()
                    ? 0
                    : numberFormatter.parse(value).longValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%d, %d)", row, column),
                    e
            );
        }
    }

    public static double getDoubleValueAt(final MeliSheet sheet, final int row, final int column) {
        try {
            final String value = getValueAt(sheet, row, column);

            return value == null || value.isEmpty()
                    ? Double.NaN
                    : numberFormatter.parse(value).doubleValue();
        } catch (ParseException | NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell in row:%d and column:%d",
                            row,
                            column
                    ), e);
        }
    }

    public static int getBigIntValueAt(final MeliRow row, final int column) {
        try {
            final BigInteger value = new BigDecimal(getValueAt(row, column)).toBigInteger();

            return value == null
                    ? BigInteger.ZERO.intValue()
                    : value.intValue();
        } catch (NullPointerException e) {
            throw new ForecastParsingException(
                    format("Error while trying to parse cell (%d, %d)", row, column),
                    e
            );
        }
    }
}