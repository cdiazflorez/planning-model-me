package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProjectionResult;
import com.mercadolibre.planning.model.me.entities.simulation.SimulationResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
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

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanningModelApiClientTest extends BaseClientTest {

    private static final String ENTITIES_URL =
            "/planning/model/workflows/fbm-wms-outbound/entities/%s";
    private static final String POST_FORECAST_URL = "/planning/model/workflows/%s/forecasts";
    private static final String CONFIGURATION_URL = "/planning/model/configuration";
    private static final String RUN_PROJECTIONS_URL = "/planning/model/workflows/%s/projections";
    private static final String RUN_SIMULATIONS_URL = "/planning/model/"
            + "workflows/%s/simulations/run";
    private static final String SAVE_SIMULATIONS_URL = "/planning/model/"
            + "workflows/%s/simulations/save";
    private static final String PLANNING_DISTRIBUTION_URL =
            "/planning/model/workflows/%s/planning_distribution";

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
                .entityType(HEADCOUNT)
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
    void testPostForecastOk() throws JSONException {
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
    void testPostForecastError() throws IOException {
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
    void testRunProjection() throws JSONException {
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
        final List<ProjectionResult> projections = client.runProjection(request);

        // Then
        assertEquals(3, projections.size());

        final ProjectionResult cpt1 = projections.get(0);
        assertEquals(ZonedDateTime.parse("2020-09-29T08:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt1.getProjectedEndDate());
        assertEquals(0, cpt1.getRemainingQuantity());

        final ProjectionResult cpt2 = projections.get(1);
        assertEquals(ZonedDateTime.parse("2020-09-29T10:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt2.getProjectedEndDate());
        assertEquals(0, cpt2.getRemainingQuantity());

        final ProjectionResult cpt3 = projections.get(2);
        assertEquals(ZonedDateTime.parse("2020-09-29T14:00:00Z", ISO_OFFSET_DATE_TIME),
                cpt3.getProjectedEndDate());
        assertEquals(70, cpt3.getRemainingQuantity());
    }

    @Test
    void testRunSimulation() throws JSONException {
        // GIVEN
        final SimulationRequest request = mockSimulationRequest();

        final JSONArray apiResponse = new JSONArray()
                .put(new JSONObject()
                        .put("date", "2020-07-27T11:00:00Z")
                        .put("projected_end_date", "2020-07-27T10:00:00Z")
                        .put("simulated_end_date", "2020-07-27T09:00:00Z")
                        .put("remaining_quantity", "1000")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T12:00:00Z")
                        .put("projected_end_date", "2020-07-27T10:40:00Z")
                        .put("simulated_end_date", "2020-07-27T09:30:00Z")
                        .put("remaining_quantity", "5000")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T03:00:00Z")
                        .put("projected_end_date", "2020-07-27T02:15:00Z")
                        .put("simulated_end_date", "2020-07-27T01:40:00Z")
                        .put("remaining_quantity", "2100")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T05:00:00Z")
                        .put("projected_end_date", "2020-07-27T06:00:00Z")
                        .put("simulated_end_date", "2020-07-27T05:10:00Z")
                        .put("remaining_quantity", "1700")
                );

        MockResponse.builder()
                .withMethod(POST)
                .withURL(format(BASE_URL + RUN_SIMULATIONS_URL, FBM_WMS_OUTBOUND))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(apiResponse.toString())
                .build();


        // When
        final List<SimulationResult> simulations = client.runSimulation(request);

        // Then
        assertEquals(4, simulations.size());

        final SimulationResult sim1 = simulations.get(0);
        assertEquals(ZonedDateTime.parse("2020-07-27T11:00:00Z", ISO_OFFSET_DATE_TIME),
                sim1.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T10:00:00Z", ISO_OFFSET_DATE_TIME),
                sim1.getProjectedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T09:00:00Z", ISO_OFFSET_DATE_TIME),
                sim1.getSimulatedEndDate());
        assertEquals(1000, sim1.getRemainingQuantity());

        final SimulationResult sim2 = simulations.get(1);
        assertEquals(ZonedDateTime.parse("2020-07-27T12:00:00Z", ISO_OFFSET_DATE_TIME),
                sim2.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T10:40:00Z", ISO_OFFSET_DATE_TIME),
                sim2.getProjectedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T09:30:00Z", ISO_OFFSET_DATE_TIME),
                sim2.getSimulatedEndDate());
        assertEquals(5000, sim2.getRemainingQuantity());

        final SimulationResult sim3 = simulations.get(2);
        assertEquals(ZonedDateTime.parse("2020-07-27T03:00:00Z", ISO_OFFSET_DATE_TIME),
                sim3.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T02:15:00Z", ISO_OFFSET_DATE_TIME),
                sim3.getProjectedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T01:40:00Z", ISO_OFFSET_DATE_TIME),
                sim3.getSimulatedEndDate());
        assertEquals(2100, sim3.getRemainingQuantity());

        final SimulationResult sim4 = simulations.get(3);
        assertEquals(ZonedDateTime.parse("2020-07-27T05:00:00Z", ISO_OFFSET_DATE_TIME),
                sim4.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T06:00:00Z", ISO_OFFSET_DATE_TIME),
                sim4.getProjectedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T05:10:00Z", ISO_OFFSET_DATE_TIME),
                sim4.getSimulatedEndDate());
        assertEquals(1700, sim4.getRemainingQuantity());
    }

    @Test
    void testSaveSimulation() throws JSONException {
        // GIVEN
        final SimulationRequest request = mockSimulationRequest();

        final JSONArray apiResponse = new JSONArray()
                .put(new JSONObject()
                        .put("date", "2020-07-27T11:00:00Z")
                        .put("projected_end_date", "2020-07-27T10:00:00Z")
                        .put("remaining_quantity", "1000")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T12:00:00Z")
                        .put("projected_end_date", "2020-07-27T10:40:00Z")
                        .put("remaining_quantity", "5000")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T03:00:00Z")
                        .put("projected_end_date", "2020-07-27T02:15:00Z")
                        .put("remaining_quantity", "2100")
                )
                .put(new JSONObject()
                        .put("date", "2020-07-27T05:00:00Z")
                        .put("projected_end_date", "2020-07-27T06:00:00Z")
                        .put("remaining_quantity", "1700")
                );

        MockResponse.builder()
                .withMethod(POST)
                .withURL(format(BASE_URL + SAVE_SIMULATIONS_URL, FBM_WMS_OUTBOUND))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(apiResponse.toString())
                .build();

        // When
        final List<SimulationResult> simulations = client.saveSimulation(request);

        // Then
        assertEquals(4, simulations.size());

        final SimulationResult sim1 = simulations.get(0);
        assertEquals(null,
                sim1.getSimulatedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T11:00:00Z", ISO_OFFSET_DATE_TIME),
                sim1.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T10:00:00Z", ISO_OFFSET_DATE_TIME),
                sim1.getProjectedEndDate());
        assertEquals(1000, sim1.getRemainingQuantity());

        final SimulationResult sim2 = simulations.get(1);
        assertEquals(null,
                sim2.getSimulatedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T12:00:00Z", ISO_OFFSET_DATE_TIME),
                sim2.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T10:40:00Z", ISO_OFFSET_DATE_TIME),
                sim2.getProjectedEndDate());
        assertEquals(5000, sim2.getRemainingQuantity());

        final SimulationResult sim3 = simulations.get(2);
        assertEquals(null,
                sim3.getSimulatedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T03:00:00Z", ISO_OFFSET_DATE_TIME),
                sim3.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T02:15:00Z", ISO_OFFSET_DATE_TIME),
                sim3.getProjectedEndDate());
        assertEquals(2100, sim3.getRemainingQuantity());

        final SimulationResult sim4 = simulations.get(3);
        assertEquals(null,
                sim4.getSimulatedEndDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T05:00:00Z", ISO_OFFSET_DATE_TIME),
                sim4.getDate());
        assertEquals(ZonedDateTime.parse("2020-07-27T06:00:00Z", ISO_OFFSET_DATE_TIME),
                sim4.getProjectedEndDate());
        assertEquals(1700, sim4.getRemainingQuantity());
    }

    private SimulationRequest mockSimulationRequest() {
        return SimulationRequest
                .builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .processName(List.of(
                        PACKING,
                        PICKING
                ))
                .dateFrom(ZonedDateTime.parse("2020-07-27T09:00:00Z"))
                .dateTo(ZonedDateTime.parse("2020-07-28T09:00:00Z"))
                .backlog(List.of(
                        QuantityByDate
                                .builder()
                                .date(ZonedDateTime.parse("2020-07-27T11:00:00Z"))
                                .quantity(15002)
                                .build(),
                        QuantityByDate
                                .builder()
                                .date(ZonedDateTime.parse("2020-07-27T12:00:00Z"))
                                .quantity(1500)
                                .build()
                ))
                .simulations(List.of(
                        Simulation
                                .builder()
                                .processName(PICKING)
                                .entities(List.of(
                                        SimulationEntity
                                                .builder()
                                                .type(HEADCOUNT)
                                                .values(List.of(
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T10:00:00Z"))
                                                                .quantity(32)
                                                                .build(),
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T14:00:00Z"))
                                                                .quantity(32)
                                                                .build()
                                                )).build(),

                                        SimulationEntity
                                                .builder()
                                                .type(PRODUCTIVITY)
                                                .values(List.of(
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T10:00:00Z"))
                                                                .quantity(40)
                                                                .build(),
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T14:00:00Z"))
                                                                .quantity(60)
                                                                .build()
                                                )).build()
                                )).build(),
                        Simulation
                                .builder()
                                .processName(PACKING)
                                .entities(List.of(
                                        SimulationEntity
                                                .builder()
                                                .type(HEADCOUNT)
                                                .values(List.of(
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T10:00:00Z"))
                                                                .quantity(32)
                                                                .build(),
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T14:00:00Z"))
                                                                .quantity(32)
                                                                .build()
                                                )).build(),

                                        SimulationEntity
                                                .builder()
                                                .type(PRODUCTIVITY)
                                                .values(List.of(
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T10:00:00Z"))
                                                                .quantity(40)
                                                                .build(),
                                                        QuantityByDate
                                                                .builder()
                                                                .date(ZonedDateTime.parse(
                                                                        "2020-07-27T14:00:00Z"))
                                                                .quantity(60)
                                                                .build()
                                                )).build()
                                )).build()
                )).build();
    }

    @Test
    void testGetConfigurationOk() throws JSONException {
        // GIVEN
        final ConfigurationRequest request = ConfigurationRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .key("estimated_delivery_time")
                .build();

        final JSONObject response = new JSONObject()
                .put("value", "60")
                .put("metric_unit", "minutes");

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL + CONFIGURATION_URL)
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(response.toString())
                .build();

        // WHEN
        final ConfigurationResponse configurationResponse = client.getConfiguration(request);

        // THEN
        assertNotNull(configurationResponse);
        assertEquals(60, configurationResponse.getValue());
        assertEquals(MINUTES, configurationResponse.getMetricUnit());
    }

    @Test
    public void testGetPlanningDistributionOk() throws JSONException {
        // GIVEN
        final ZonedDateTime currentTime =
                ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
        final ZonedDateTime cpt1 = currentTime.plusHours(4);
        final ZonedDateTime cpt2 = currentTime.plusHours(5);
        final ZonedDateTime cpt3 = currentTime.plusHours(6);

        final PlanningDistributionRequest request = new PlanningDistributionRequest(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND,
                currentTime,
                currentTime.plusHours(1)
        );

        final JSONArray response = new JSONArray()
                .put(new JSONObject()
                        .put("total", "7501")
                        .put("metric_unit", "units")
                        .put("date_in", currentTime.format(ISO_OFFSET_DATE_TIME))
                        .put("date_out", cpt1.format(ISO_OFFSET_DATE_TIME))
                )
                .put(new JSONObject()
                        .put("total", "100")
                        .put("metric_unit", "units")
                        .put("date_in", currentTime.format(ISO_OFFSET_DATE_TIME))
                        .put("date_out", cpt2.format(ISO_OFFSET_DATE_TIME))
                )
                .put(new JSONObject()
                        .put("total", "1212")
                        .put("metric_unit", "units")
                        .put("date_in", currentTime.format(ISO_OFFSET_DATE_TIME))
                        .put("date_out", cpt3.format(ISO_OFFSET_DATE_TIME))
                );

        MockResponse.builder()
                .withMethod(GET)
                .withURL(format(BASE_URL + PLANNING_DISTRIBUTION_URL, FBM_WMS_OUTBOUND.getName()))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(response.toString())
                .build();

        // WHEN
        final List<PlanningDistributionResponse> planningDistributionResponses =
                client.getPlanningDistribution(request);

        // THEN
        assertEquals(3, planningDistributionResponses.size());

        final PlanningDistributionResponse cpt1Response = planningDistributionResponses.get(0);
        final PlanningDistributionResponse cpt2Response = planningDistributionResponses.get(1);
        final PlanningDistributionResponse cpt3Response = planningDistributionResponses.get(2);

        assertEquals(7501, cpt1Response.getTotal());
        assertEquals(UNITS, cpt1Response.getMetricUnit());
        assertEquals(currentTime.format(ISO_OFFSET_DATE_TIME),
                cpt1Response.getDateIn().format(ISO_OFFSET_DATE_TIME));
        assertEquals(cpt1.format(ISO_OFFSET_DATE_TIME),
                cpt1Response.getDateOut().format(ISO_OFFSET_DATE_TIME));

        assertEquals(100, cpt2Response.getTotal());
        assertEquals(UNITS, cpt2Response.getMetricUnit());
        assertEquals(currentTime.format(ISO_OFFSET_DATE_TIME),
                cpt2Response.getDateIn().format(ISO_OFFSET_DATE_TIME));
        assertEquals(cpt2.format(ISO_OFFSET_DATE_TIME),
                cpt2Response.getDateOut().format(ISO_OFFSET_DATE_TIME));

        assertEquals(1212, cpt3Response.getTotal());
        assertEquals(UNITS, cpt3Response.getMetricUnit());
        assertEquals(currentTime.format(ISO_OFFSET_DATE_TIME),
                cpt3Response.getDateIn().format(ISO_OFFSET_DATE_TIME));
        assertEquals(cpt3.format(ISO_OFFSET_DATE_TIME),
                cpt3Response.getDateOut().format(ISO_OFFSET_DATE_TIME));
    }
}
