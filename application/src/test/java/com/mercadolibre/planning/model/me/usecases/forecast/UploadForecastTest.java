package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastCreationResponse;
import com.mercadolibre.planning.model.me.usecases.forecast.dto.ForecastDto;
import com.mercadolibre.planning.model.me.usecases.forecast.parsers.Target;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UploadForecastTest {
  private static final String VALID_FILE_PATH = "outbound_forecast.xlsx";

  private static final LogisticCenterConfiguration CONFIG =
      new LogisticCenterConfiguration(TimeZone.getDefault());

  @InjectMocks
  private UploadForecast uploadForecast;

  @Mock
  private CreateForecast createForecast;

  @Mock
  private LogisticCenterGateway logisticCenterGateway;

  @Test
  void testUploadOk() {
    // GIVEN
    final byte[] bytes = TestUtils.getResource(VALID_FILE_PATH);
    var expectedResponse = new ForecastCreationResponse("ok");

    when(logisticCenterGateway.getConfiguration(anyString())).thenReturn(CONFIG);

    when(createForecast.execute(any(ForecastDto.class))).thenReturn(expectedResponse);

    // WHEN
    var response = uploadForecast.upload(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        Target.FBM_WMS_OUTBOUND.forecastParser,
        bytes,
        1234L
    );

    // VERIFY
    assertEquals(expectedResponse, response);
  }
}
