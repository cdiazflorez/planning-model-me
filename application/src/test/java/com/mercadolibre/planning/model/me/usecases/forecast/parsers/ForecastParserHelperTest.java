package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.SalesDistributionSheetParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocument;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ForecastParserHelperTest {

    private static final LogisticCenterConfiguration CONF = new LogisticCenterConfiguration(TimeZone.getDefault());

    @Test
    void sheetNotFoundShouldThrowForecastParsingException() {
        // GIVEN
        final MeliDocument document = createMeliDocument(List.of("Invalid sheet name"));

        // WHEN - THEN
        assertThrows(ForecastParsingException.class, () -> ForecastParserHelper.parseSheets(
                document,
                Stream.of(new SalesDistributionSheetParser()),
                WAREHOUSE_ID,
                CONF
        ));
    }
}
