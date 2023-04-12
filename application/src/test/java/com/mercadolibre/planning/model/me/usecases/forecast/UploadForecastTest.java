package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.exception.ForecastParsingException;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.Target;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UploadForecastTest {
  private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";

  private static final String INVALID_FILE = "forecast-brba01-error-pp.xlsx";

  private static final LogisticCenterConfiguration CONFIG =
      new LogisticCenterConfiguration(TimeZone.getDefault());

  @InjectMocks
  private UploadForecast uploadForecast;

  @Mock
  private CreateForecast createForecast;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Mock
  private UploadForecast.ProcessPathGateway processPathGateway;

  @Test
  void testUploadOk() {
    // GIVEN
    final byte[] bytes = TestUtils.getResource(VALID_FILE_PATH);
    var expectedResponse = new ForecastCreationResponse("ok");

    when(logisticCenterGateway.getConfiguration(anyString())).thenReturn(CONFIG);

    when(createForecast.execute(any(ForecastDto.class))).thenReturn(expectedResponse);

    // WHEN
    var response = uploadForecast.upload(
        "ARBA01",
        FBM_WMS_OUTBOUND,
        Target.FBM_WMS_OUTBOUND.forecastParser,
        bytes,
        1234L
    );

    // VERIFY
    assertEquals(expectedResponse, response);
  }

  @Test
  @DisplayName("Excel with invalid process path")
  void parseFileWithInvalidProcessPath() {
    // GIVEN
    final byte[] bytes = TestUtils.getResource(INVALID_FILE);

    when(logisticCenterGateway.getConfiguration(anyString())).thenReturn(CONFIG);

    when(processPathGateway.getProcessPathGateway("BRBA01"))
        .thenReturn(Set.of(TOT_MONO, TOT_MULTI_BATCH));

    //WHEN //THEN
    final ForecastParsingException exception = assertThrows(ForecastParsingException.class,
        () -> uploadForecast.upload(
            "BRBA01",
            FBM_WMS_OUTBOUND,
            Target.FBM_WMS_OUTBOUND.forecastParser,
            bytes,
            1234L
        )
    );

    assertNotNull(exception.getMessage());
    final String expectedPartMessage = "is an invalid process path for this warehouse. PP available : ";

    assertTrue(exception.getMessage().contains(expectedPartMessage));
  }
}
