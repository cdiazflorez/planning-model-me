package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.createMeliDocumentFrom;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ParseInboundForecastFromFileTest {
    private static final String VALID_FILE_PATH = "inbound_planning_ok.xlsx";

    private static final LogisticCenterConfiguration CONFIG =
            new LogisticCenterConfiguration(TimeZone.getDefault());

    @Test
    void testUploadForecastOk() {
        // GIVEN
        var document = createMeliDocumentFrom(getResource(VALID_FILE_PATH));

        // WHEN
        final Forecast forecast = ParseInboundForecastFromFile.parse("ARBA01", document, 1234L, CONFIG);

        // THEN
        assertNotNull(forecast);
        assertEquals(2, forecast.getMetadata().size());
        assertEquals("ARBA01", forecast.getMetadata().get(0).getValue());
    }
}
