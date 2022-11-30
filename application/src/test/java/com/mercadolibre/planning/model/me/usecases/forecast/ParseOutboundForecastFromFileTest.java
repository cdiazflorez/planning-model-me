package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class ParseOutboundForecastFromFileTest {

    private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";
    private static final LogisticCenterConfiguration CONFIG =
            new LogisticCenterConfiguration(TimeZone.getDefault());

    @Test
    void testUploadForecastOk() {
        // GIVEN
        final var document = SpreadsheetUtils.createMeliDocumentFrom(
                TestUtils.getResource(VALID_FILE_PATH)
        );
        // WHEN
        final var forecast = ParseOutboundForecastFromFile.parse("ARBA01", document, 1234L, CONFIG, logisticCenter -> true);

        // THEN
        assertNotNull(forecast);
        assertEquals("ARBA01", forecast.getMetadata().get(0).getValue());
        assertEquals("14-2022", forecast.getMetadata().get(1).getValue());
        assertEquals(1234L, forecast.getUserID());
    }
}
