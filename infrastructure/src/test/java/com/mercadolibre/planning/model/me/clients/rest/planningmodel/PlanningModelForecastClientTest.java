package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.mockPostUrlSuccess;
import static com.mercadolibre.planning.model.me.utils.TestUtils.objectMapper;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlanningModelForecastClientTest extends BaseClientTest {

    private static final String POST_FORECAST_URL = "/planning/model/workflows/%s/forecasts";

    private PlanningModelForecastClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new PlanningModelForecastClient(getRestTestClient(), objectMapper());
    }

    @AfterEach
    void tearDown() {
        super.cleanMocks();
    }

    @Test
    void testPostForecastOk() throws JSONException {
        // GIVEN
        final String date = new Date().toString();
        final Forecast forecast = Forecast.builder()
                .metadata(List.of(
                        new Metadata("warehouse_id", WAREHOUSE_ID)
                ))
                .build();
        final JSONObject request = new JSONObject()
                .put("workflow", FBM_WMS_OUTBOUND)
                .put("last_update", date)
                .put("metadata", new JSONArray()
                        .put(new JSONObject().put("warehouse_id", WAREHOUSE_ID))
                );

        mockPostUrlSuccess(format(BASE_URL + POST_FORECAST_URL, FBM_WMS_OUTBOUND), request);

        // WHEN - THEN
        assertDoesNotThrow(() -> client.postForecast(FBM_WMS_OUTBOUND, forecast));
    }

    @Test
    void testPostForecastError() throws IOException {
        // GIVEN
        final ObjectMapper mockedObjectMapper = mock(ObjectMapper.class);
        this.client = new PlanningModelForecastClient(getRestTestClient(), mockedObjectMapper);
        final Forecast forecast = Forecast.builder()
                .metadata(List.of(
                        new Metadata("warehouse_id", WAREHOUSE_ID)
                ))
                .build();

        when(mockedObjectMapper.writeValueAsBytes(forecast))
                .thenThrow(JsonProcessingException.class);

        // WHEN - THEN
        assertThrows(ClientException.class,() -> client.postForecast(FBM_WMS_OUTBOUND, forecast));
    }

}
