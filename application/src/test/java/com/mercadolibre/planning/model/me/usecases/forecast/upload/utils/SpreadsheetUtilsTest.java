package com.mercadolibre.planning.model.me.usecases.forecast.upload.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocument;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocumentAsByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpreadsheetUtilsTest {
    private static final String HOUR_MINUTE_FORMAT_PATTERN = "^([0]?[0-9]|[0-9][0-9]):[0-5][0-9]$";

    private Exception exception;

    @Test
    void testCreateMeliDocumentFromOk() {
        // GIVEN
        final byte[] validByteArray = createMeliDocumentAsByteArray(List.of(WORKERS.getName()));

        // WHEN
        final MeliDocument document = SpreadsheetUtils.createMeliDocumentFrom(validByteArray);

        assertNotNull(document);
        assertEquals(1, document.getSheets().size());
        assertNotNull(document.getSheetByName(WORKERS.getName()));
    }

    @Test
    void testCreateMeliDocumentFromError() {
        assertThrows(ForecastParsingException.class,
                () -> SpreadsheetUtils.createMeliDocumentFrom(new byte[1]));
    }

    @Test
    void testGetIntValueAtError() {
        // GIVEN
        final MeliRow row = createMeliDocument(List.of("Test")).getSheetByName("Test").addRow();

        final int result = SpreadsheetUtils.getIntValueAt(row, 1);

        assertEquals(0, result);
    }

    @Test
    void testGetIntValueAtOnEmpty() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell();

        // WHEN
        final int result = SpreadsheetUtils.getIntValueAt(sheet, 0, 0);

        assertEquals(0, result);
    }

    @Test
    void testGetLongValueAtOnEmptyRowAndColumn() {
        // GIVEN
        final MeliRow row = createMeliDocument(List.of("Test")).getSheetByName("Test").addRow();

        // WHEN
        final long result = SpreadsheetUtils.getLongValueAt(row, 0);

        assertEquals(0, result);
    }

    @Test
    void testGetLongValueAtOnEmptySheetRowAndColumn() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell();

        // WHEN
        final long result = SpreadsheetUtils.getLongValueAt(sheet, 0, 0);

        assertEquals(0L, result);
    }

    @Test
    void testGetDoubleValueAtOnEmptySheetRowAndColumn() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell();

        // WHEN
        final double result = SpreadsheetUtils.getDoubleValueAt(sheet, 0, 0);

        assertEquals(0.00, result);
    }

    @Test
    void testGetDateTimeAtSuccess() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell().setValue("12/12/2020 08:00");

        // WHEN
        final ZonedDateTime result = SpreadsheetUtils.getDateTimeAt(sheet, 0, 0,
                ZoneId.systemDefault());

        assertNotNull(result);

        assertEquals(2020, result.get(ChronoField.YEAR));
        assertEquals(12, result.get(ChronoField.MONTH_OF_YEAR));
        assertEquals(12, result.get(ChronoField.DAY_OF_MONTH));
    }

    @Test
    void testGetDateTimeAtError() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell().setValue("24/12/2020 08:00");

        // WHEN
        try {
            SpreadsheetUtils.getDateTimeAt(sheet, 0, 0, ZoneId.systemDefault());
        } catch (Exception e) {
            exception = e;
        }
    }
    
    @Test
    void testGetDurationValueSuccess() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell().setValue("01:18");

        // WHEN
        final Duration result = SpreadsheetUtils.getDurationAt(sheet, 0, 0,
                HOUR_MINUTE_FORMAT_PATTERN);

        assertNotNull(result);
        assertEquals(18, result.toMinutesPart());
        assertEquals(1, result.toHoursPart());
    }

    @Test
    void testGetDurationValueFailedByInputFormat() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell().setValue("01:18:00");

        // WHEN
        try {
            SpreadsheetUtils.getDurationAt(sheet, 0, 0, HOUR_MINUTE_FORMAT_PATTERN);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ForecastParsingException);
    }

    @Test
    void testGetIntValueFromDurationValueSuccess() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");
        sheet.addRow().addCell().setValue("01:18");

        // WHEN
        final int result = SpreadsheetUtils.getIntValueAtFromDuration(sheet, 0, 0);

        assertEquals(78, result);
    }

    @Test
    void testGetIntValueFromDurationValueFailed() {
        // GIVEN
        final MeliSheet sheet = createMeliDocument(List.of("Test")).getSheetByName("Test");

        // WHEN
        try {
            SpreadsheetUtils.getIntValueAtFromDuration(sheet, 0, 0);
        } catch (ForecastParsingException e) {
            exception = e;
        }

        assertNotNull(exception);
        assertTrue(exception instanceof ForecastParsingException);
    }
}
