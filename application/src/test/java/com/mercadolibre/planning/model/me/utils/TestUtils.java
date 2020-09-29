package com.mercadolibre.planning.model.me.utils;

import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliDocumentFactory;
import com.mercadolibre.spreadsheet.MeliSheet;
import com.mercadolibre.spreadsheet.implementations.poi.PoiDocument;
import com.mercadolibre.spreadsheet.implementations.poi.PoiMeliDocumentFactory;

import java.io.IOException;
import java.util.List;

public class TestUtils {

    public static final String WAREHOUSE_ID = "ARTW01";
    public static final Long USER_ID = 1234L;
    private static final String FORECAST_EXAMPLE_FILE = "forecast_example.xlsx";

    private static MeliDocumentFactory meliDocumentFactory = new PoiMeliDocumentFactory();

    public static MeliDocument createMeliDocument(final List<String> sheetNames) {
        try {
            final MeliDocument meliDocument = meliDocumentFactory.newDocument();

            sheetNames.forEach((sheetName) -> meliDocument.addSheet(sheetName));

            return meliDocument;
        } catch (final MeliDocument.MeliDocumentException e) {
            return null;
        }
    }

    public static byte[] createMeliDocumentAsByteArray(final List<String> sheetNames) {
        try {
            return createMeliDocument(sheetNames).toBytes();
        } catch (final MeliDocument.MeliDocumentException e) {
            return null;
        }
    }

    public static MeliSheet getMeliSheetFromTestFile(String name) {
        final byte[] forecastExampleFile = getResource(FORECAST_EXAMPLE_FILE);

        try {
            return new PoiDocument(forecastExampleFile).getSheetByName(name);
        } catch (MeliDocument.MeliDocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getResource(final String resourceName) {
        try {
            return TestUtils.class.getClassLoader()
                    .getResourceAsStream(resourceName).readAllBytes();
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
