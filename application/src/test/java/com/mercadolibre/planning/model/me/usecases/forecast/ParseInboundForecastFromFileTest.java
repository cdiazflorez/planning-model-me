package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.usecases.forecast.ParseInboundForecastFromFile;
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
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.createMeliDocumentAsByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParseInboundForecastFromFileTest {

    private static final String STAFFING_SHEET = "Plan de Staffing";
    private static final String SHIPMENTS_SHEET = "Distribución Iss";

    @InjectMocks
    private ParseInboundForecastFromFile parseForecastFromFile;

    @Mock
    private ForecastParser forecastParser;

    @Test
    void testUploadForecastOk() {
        // GIVEN
        final List<String> sheetNames = List.of(STAFFING_SHEET, SHIPMENTS_SHEET);
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
        assertEquals(2, forecast.getMetadata().size());
        assertEquals(WAREHOUSE_ID, forecast.getMetadata().get(0).getValue());
    }

    private List<ForecastSheetDto> getSheetDtos(final List<String> sheetNames) {
        return sheetNames.stream()
                .map(sheetName -> new ForecastSheetDto(sheetName, Map.of()))
                .collect(Collectors.toList());
    }
}
