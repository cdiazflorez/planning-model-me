package com.mercadolibre.planning.model.me.usecases.forecast.upload.utils;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliRow;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocument;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocumentAsByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpreadsheetUtilsTest {

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
        final MeliRow row = createMeliDocument(List.of("Test"))
                .getSheetByName("Test")
                .addRow();

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
        final MeliRow row = createMeliDocument(List.of("Test"))
                .getSheetByName("Test")
                .addRow();

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
}
