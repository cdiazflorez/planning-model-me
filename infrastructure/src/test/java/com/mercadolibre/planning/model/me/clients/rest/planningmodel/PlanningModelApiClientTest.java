package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Backlog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.restclient.MockResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.mockPostUrlSuccess;
import static com.mercadolibre.planning.model.me.utils.TestUtils.objectMapper;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlanningModelApiClientTest extends BaseClientTest {

    private static final String ENTITIES_URL =
            "/planning/model/workflows/fbm-wms-outbound/entities/%s";
    private static final String POST_FORECAST_URL = "/planning/model/workflows/%s/forecasts";
    private static final String RUN_PROJECTIONS_URL = "/planning/model/workflows/%s/projections";

    private PlanningModelApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new PlanningModelApiClient(getRestTestClient(), objectMapper());
    }

    @AfterEach
    void tearDown() {
        super.cleanMocks();
    }

    @Test
    void testGetEntities() throws JSONException {
        // Given
        final EntityRequest request = EntityRequest.builder()
                .entityType(EntityType.HEADCOUNT)
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId("ARTW01")
                .dateFrom(ZonedDateTime.now())
                .dateTo(ZonedDateTime.now().plusDays(1))
                .source(Source.FORECAST)
                .processName(List.of(PICKING, PACKING))
                .build();

        final JSONArray apiResponse = new JSONArray()
                .put(new JSONObject()
                        .put("date", request.getDateFrom().format(ISO_OFFSET_DATE_TIME))
                        .put("workflow", "fbm-wms-outbound")
                        .put("process_name", "picking")
                        .put("value", "30")
                        .put("source", "forecast")
                        .put("metric_unit", "minutes")
                )
                .put(new JSONObject()
                        .put("date", request.getDateTo().format(ISO_OFFSET_DATE_TIME))
                        .put("workflow", "fbm-wms-outbound")
                        .put("process_name", "packing")
                        .put("value", "20")
                        .put("source", "simulation")
                        .put("metric_unit", "percentage")
                );
        mockGetEntity(apiResponse);

        // When
        final List<Entity> headcounts = client.getEntities(request);

        // Then
        assertEquals(2, headcounts.size());

        final Entity headcount0 = headcounts.get(0);
        assertTrue(request.getDateFrom().isEqual(headcount0.getDate()));
        assertEquals(FBM_WMS_OUTBOUND, headcount0.getWorkflow());
        assertEquals(PICKING, headcount0.getProcessName());
        assertEquals(30, headcount0.getValue());
        assertEquals(Source.FORECAST, headcount0.getSource());

        final Entity headcount1 = headcounts.get(1);
        assertTrue(request.getDateTo().isEqual(headcount1.getDate()));
        assertEquals(FBM_WMS_OUTBOUND, headcount1.getWorkflow());
        assertEquals(PACKING, headcount1.getProcessName());
        assertEquals(20, headcount1.getValue());
        assertEquals(Source.SIMULATION, headcount1.getSource());
    }

    private void mockGetEntity(final JSONArray response) {
        MockResponse.builder()
                .withMethod(GET)
                .withURL(format(BASE_URL + ENTITIES_URL, "headcount"))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(response.toString())
                .build();
    }

    @Test
    public void testPostForecastOk() throws JSONException {
        // GIVEN
        final String date = new Date().toString();
        final Forecast forecast = Forecast.builder()
                .metadata(List.of(
                        Metadata.builder()
                                .key("warehouse_id")
                                .value(WAREHOUSE_ID)
                                .build()
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
    public void testPostForecastError() throws IOException {
        // GIVEN
        final ObjectMapper mockedObjectMapper = mock(ObjectMapper.class);
        this.client = new PlanningModelApiClient(getRestTestClient(), mockedObjectMapper);
        final Forecast forecast = Forecast.builder()
                .metadata(List.of(
                        Metadata.builder()
                                .key("warehouse_id")
                                .value(WAREHOUSE_ID)
                                .build()
                ))
                .build();

        when(mockedObjectMapper.writeValueAsBytes(forecast))
                .thenThrow(JsonProcessingException.class);

        // WHEN - THEN
        assertThrows(ClientException.class,() -> client.postForecast(FBM_WMS_OUTBOUND, forecast));
    }

    @Test
    public void testRunProjection() throws JSONException {
        // GIVEN
        final ProjectionRequest request = ProjectionRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .type(ProjectionType.CPT)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(ZonedDateTime.now())
                .dateTo(ZonedDateTime.now().plusDays(1))
                .backlog(List.of(
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-09-29T10:00:00Z"))
                                .quantity(100)
                                .build(),
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-09-29T11:00:00Z"))
                                .quantity(200)
                                .build(),
                        Backlog.builder()
                                .date(ZonedDateTime.parse("2020-09-29T12:00:00Z"))
                                .quantity(300)
                                .build()
                ))
                .build();

        final JSONArray apiResponse = new JSONArray()
                .put(new JSONObject()
                        .put("date", "2020-09-29T10:00:00Z")
                        .put("projected_end_date", "2020-09-29T08:00:00Z")
                        .put("remaining_quantity", "0")
                )
                .put(new JSONObject()
                        .put("date", "2020-09-29T11:00:00Z")
                        .put("projected_end_date", "2020-09-29T10:00:00Z")
                        .put("remaining_quantity", "0")
                )
                .put(new JSONObject()
                        .put("date", "2020-09-29T12:00:00Z")
                        .put("projected_end_date", "2020-09-29T14:00:00Z")
                        .put("remaining_quantity", "70")
                );

        MockResponse.builder()
                .withMethod(POST)
                .withURL(format(BASE_URL + RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(apiResponse.toString())
                .build();

        // When
        final List<ProjectionResponse> projections = client.runProjection(request);

        // Then
        assertEquals(3, projections.size());

        final ProjectionResponse cpt1 = projections.get(0);
        assertEquals(ZonedDateTime.parse("2020-09-29T08:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt1.getProjectedEndDate());
        assertEquals(0, cpt1.getRemainingQuantity());

        final ProjectionResponse cpt2 = projections.get(1);
        assertEquals(ZonedDateTime.parse("2020-09-29T10:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt2.getProjectedEndDate());
        assertEquals(0, cpt2.getRemainingQuantity());

        final ProjectionResponse cpt3 = projections.get(2);
        assertEquals(ZonedDateTime.parse("2020-09-29T14:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt3.getProjectedEndDate());
        assertEquals(70, cpt3.getRemainingQuantity());
    }
}
