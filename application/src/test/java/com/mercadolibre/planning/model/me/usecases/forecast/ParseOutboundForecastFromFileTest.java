package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastSheetDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.ForecastParser;
import com.mercadolibre.spreadsheet.MeliDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.ORDER_DISTRIBUTION;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.outbound.model.ForecastSheet.WORKERS;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocumentAsByteArray;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParseOutboundForecastFromFileTest {

    @InjectMocks
    private ParseOutboundForecastFromFile parseForecastFromFile;

    @Mock
    private ForecastParser forecastParser;

    @Test
    void testUploadForecastOk() {
        // GIVEN
        final List<String> sheetNames = List.of(WORKERS.getName(), ORDER_DISTRIBUTION.getName());
        final FileUploadDto input = new FileUploadDto(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND,
                createMeliDocumentAsByteArray(sheetNames),
                1234L);

        when(forecastParser.parse(eq(WAREHOUSE_ID), eq(FBM_WMS_OUTBOUND), any(MeliDocument.class)))
                .thenReturn(getSheetDtos(sheetNames));

        // WHEN
        final Forecast forecast = parseForecastFromFile.execute(input);

        // THEN
        assertNotNull(forecast);
    }

    private static List<ForecastSheetDto> getSheetDtos(final List<String> sheetNames) {
        return sheetNames.stream()
                .map(sheetName -> new ForecastSheetDto(sheetName, Map.of()))
                .collect(Collectors.toList());
    }
}
