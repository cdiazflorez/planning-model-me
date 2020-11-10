package com.mercadolibre.planning.model.me.usecases.forecast.upload.parsers;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.usecases.forecast.upload.dto.ForecastSheetDto;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastColumnName.MONO_ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.upload.workflow.wms.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultForecastParserTest {

    @InjectMocks
    private DefaultForecastParser defaultForecastParser;

    @Mock
    private Set<SheetParser> sheetParsers;

    @Test
    void parseOk() {
        // GIVEN
        final MeliDocument document = createMeliDocument(List.of(
                WORKERS.getName(),
                ORDER_DISTRIBUTION.getName()
        ));
        final SheetParser mockedSheetParser1 = mock(SheetParser.class);
        final SheetParser mockedSheetParser2 = mock(SheetParser.class);

        when(sheetParsers.stream()).thenReturn(Stream.of(
                mockedSheetParser1,
                mockedSheetParser2
        ));
        when(mockedSheetParser1.name()).thenReturn(WORKERS.getName());
        when(mockedSheetParser1.parse(eq(WAREHOUSE_ID), any(MeliSheet.class)))
                .thenReturn(new ForecastSheetDto(WORKERS.getName(), Map.of(
                        MONO_ORDER_DISTRIBUTION, "58,00"
                )));
        when(mockedSheetParser2.name()).thenReturn(ORDER_DISTRIBUTION.getName());
        when(mockedSheetParser2.parse(eq(WAREHOUSE_ID), any(MeliSheet.class)))
                .thenReturn(new ForecastSheetDto(ORDER_DISTRIBUTION.getName(), Map.of(
                        MONO_ORDER_DISTRIBUTION, "58,00"
                )));

        // WHEN
        final List<ForecastSheetDto> parsedSheets =
                defaultForecastParser.parse(WAREHOUSE_ID, document);

        // THEN
        assertNotNull(parsedSheets);
        assertEquals(2, parsedSheets.size());
    }

    @Test
    void sheetNotFoundShouldThrowForecastParsingException() {
        // GIVEN
        final MeliDocument document = createMeliDocument(List.of(
                "Invalid sheet name"
        ));
        final SheetParser mockedSheetParser = mock(SheetParser.class);

        when(sheetParsers.stream()).thenReturn(Stream.of(mockedSheetParser));
        when(mockedSheetParser.name()).thenReturn(WORKERS.getName());

        // WHEN - THEN
        assertThrows(ForecastParsingException.class,
                () -> defaultForecastParser.parse(WAREHOUSE_ID, document));
    }
}
