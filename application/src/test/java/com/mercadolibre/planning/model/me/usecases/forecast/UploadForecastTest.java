package com.mercadolibre.planning.model.me.usecases.forecast;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.FileUploadDto;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UploadForecastTest {

    @InjectMocks
    private UploadForecast uploadForecast;

    @Mock
    private List<ParseForecastFromFile> parsers;

    @Mock
    private CreateForecast createForecast;

    @Test
    void testUploadOk() {
        // GIVEN
        final byte[] file = new byte[0];
        final FileUploadDto fileUploadDto =
                new FileUploadDto(WAREHOUSE_ID, FBM_WMS_OUTBOUND, file, 1234);

        final Forecast forecast = mock(Forecast.class);
        final ForecastDto forecastDto = new ForecastDto(FBM_WMS_OUTBOUND, forecast);

        final ParseForecastFromFile parser = mock(ParseForecastFromFile.class);
        when(parser.getWorkflow()).thenReturn(FBM_WMS_OUTBOUND);
        when(parser.execute(fileUploadDto)).thenReturn(forecast);

        final ForecastCreationResponse response = new ForecastCreationResponse("ok");
        when(createForecast.execute(forecastDto)).thenReturn(response);

        when(parsers.stream()).thenReturn(Stream.of(parser));

        // WHEN
        final ForecastCreationResponse result = uploadForecast.upload(
                TestUtils.WAREHOUSE_ID, FBM_WMS_OUTBOUND, file, 1234L
        );

        //THEN
        assertNotNull(result);
        assertEquals(response, result);
    }

    @Test
    void testNoParserFound() {
        // GIVEN
        final byte[] file = new byte[0];

        when(parsers.stream()).thenReturn(Stream.empty());

        // WHEN
        assertThrows(ForecastParsingException.class,
                () -> uploadForecast.upload(
                        TestUtils.WAREHOUSE_ID, FBM_WMS_OUTBOUND, file, 1234L
                ));
    }

}
