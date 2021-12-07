package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingDistribution;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.PROCESSING_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getMeliSheetFrom;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundRepsForecastSheetParserTest {

    private static final String VALID_FILE_PATH = "inbound_planning_ok.xlsx";
    private static final String ERRONEOUS_FILE_PATH = "inbound_planning_with_errors.xlsx";

    @InjectMocks
    private InboundRepsForecastSheetParser parser;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    @DisplayName("Excel Parsed Ok")
    void parseOk() {
        // GIVEN
        when(logisticCenterGateway.getConfiguration("ARBA01"))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getTimeZone("UTC")));

        final MeliSheet repsSheet = getMeliSheetFrom(parser.name(), VALID_FILE_PATH);

        // WHEN
        final ForecastSheetDto result = parser.parse("ARBA01", repsSheet);

        // THEN
        assertNotNull(result);
        assertEquals("Plan de staffing", result.getSheetName());

        final var processingDistributions = (List<ProcessingDistribution>) result.getValues()
                .get(PROCESSING_DISTRIBUTION);

        assertNotNull(processingDistributions);
        assertEquals(13, processingDistributions.size());

        final var receivingTarget = processingDistributions.get(0);
        assertEquals("receiving", receivingTarget.getProcessName());
        assertEquals(168, receivingTarget.getData().size());

        final var firstRow = receivingTarget.getData().get(0);
        final var expectedDate = ZonedDateTime.parse("2021-11-07T00:00Z", ISO_OFFSET_DATE_TIME);
        assertEquals(expectedDate, firstRow.getDate());
        assertEquals(0, firstRow.getQuantity());
    }

    @Test
    @DisplayName("Excel With Errors")
    void errors() {
        // GIVEN
        when(logisticCenterGateway.getConfiguration("ARBA01"))
                .thenReturn(new LogisticCenterConfiguration(TimeZone.getDefault()));

        final MeliSheet repsSheet = getMeliSheetFrom(parser.name(), ERRONEOUS_FILE_PATH);

        // WHEN
        final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
                () -> parser.parse("ARBA01", repsSheet));

        // THEN
        assertNotNull(exception.getMessage());

        final String expectedMessage =
                "Error while trying to parse cell (D15) for sheet: Plan de staffing.\n"
                + "Error while trying to parse cell (B27) for sheet: Plan de staffing.\n"
                + "Error while trying to parse cell (J32) for sheet: Plan de staffing";

        assertEquals(expectedMessage, exception.getMessage());
    }

}
