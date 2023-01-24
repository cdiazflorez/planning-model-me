package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.WAVING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.CARRY_OVER;
import static com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogProcessStatus.PROCESSED;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.me.utils.TestUtils.objectMapper;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception.ForecastNotFoundException;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.sharedistribution.ShareDistribution;
import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ConfigurationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Deviation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ForecastMetadataRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetDeviationResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.GetUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PlanningDistributionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Productivity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionResult;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProjectionType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveSimulationsRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SearchTrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.request.CurrentBacklog;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response.ProjectionValue;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogAreaDistribution;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantity;
import com.mercadolibre.planning.model.me.gateways.projection.backlog.BacklogQuantityAtSla;
import com.mercadolibre.planning.model.me.gateways.projection.deferral.DeferralProjectionStatus;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.dtos.GetShareDistributionInput;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PlanningModelApiClientTest extends BaseClientTest {

  private static final String ENTITIES_URL =
      "/planning/model/workflows/fbm-wms-outbound/entities/%s";

  private static final String GET_FORECAST_METADATA_URL =
      "/planning/model/workflows/%s/metadata";

  private static final String CONFIGURATION_URL = "/planning/model/configuration";

  private static final String RUN_PROJECTIONS_URL = "/planning/model/workflows/%s/projections/%s";

  private static final String BASE_SIMULATIONS_URL = "/planning/model/workflows/%s/simulations";

  private static final String RUN_SIMULATIONS_URL = BASE_SIMULATIONS_URL + "/run";

  private static final String SAVE_SIMULATIONS_URL = BASE_SIMULATIONS_URL + "/save";

  private static final String PLANNING_DISTRIBUTION_URL =
      "/planning/model/workflows/%s/planning_distributions";

  private static final String DEVIATION_URL =
      "/planning/model/workflows/%s/deviations";

  private static final String ACTIVE_DEVIATIONS_URL = "/planning/model/workflows/%s/deviations/active";

  private static final String UNITS_DISTRIBUTION = "/planning/model/workflows/%s/entities/units_distribution";

  private static final String TYPE_FIELD = "type";

  private static final String VALUE_FIELD = "value";

  private static final String PERFORMED_PROCESSING = "performed_processing";

  private PlanningModelApiClient client;

  private static Stream<Arguments> entityRequests() {
    return Stream.of(
        of(
            TrajectoriesRequest.builder()
                .entityType(HEADCOUNT)
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId("ARTW01")
                .dateFrom(now())
                .dateTo(now().plusDays(1))
                .source(Source.FORECAST)
                .processName(List.of(PICKING, PACKING))
                .build()
        ),
        of(
            TrajectoriesRequest.builder()
                .entityType(HEADCOUNT)
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId("ARTW01")
                .dateFrom(now())
                .dateTo(now().plusDays(1))
                .source(Source.FORECAST)
                .processName(List.of(PICKING, PACKING))
                .processingType(List.of(ProcessingType.ACTIVE_WORKERS))
                .build()
        )
    );
  }

  private static Stream<Arguments> errorResponseProvider() throws IOException {
    return Stream.of(
        of(ForecastNotFoundException.class,
            getResourceAsString("forecast_not_found_response.json")),
        of(ClientException.class, "")
    );
  }

  @BeforeEach
  void setUp() throws IOException {
    client = new PlanningModelApiClient(getRestTestClient(), objectMapper());
  }

  @AfterEach
  void tearDown() {
    super.cleanMocks();
  }

  @ParameterizedTest
  @MethodSource("entityRequests")
  void testGetEntities(final TrajectoriesRequest request) throws JSONException {
    // Given
    final JSONArray apiResponse = new JSONArray()
        .put(new JSONObject()
            .put("date", request.getDateFrom().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "picking")
            .put(TYPE_FIELD, "active_workers")
            .put(VALUE_FIELD, "30")
            .put("source", "forecast")
            .put("metric_unit", "minutes")
        )
        .put(new JSONObject()
            .put("date", request.getDateTo().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "packing")
            .put(TYPE_FIELD, "active_workers")
            .put(VALUE_FIELD, "20")
            .put("source", "simulation")
            .put("metric_unit", "percentage")
        );
    mockPostEntity(apiResponse);

    // When
    final List<MagnitudePhoto> headcounts = client.getTrajectories(request);

    // Then
    assertEquals(2, headcounts.size());

    final MagnitudePhoto headcount0 = headcounts.get(0);
    assertTrue(request.getDateFrom().isEqual(headcount0.getDate()));
    assertEquals(PICKING, headcount0.getProcessName());
    assertEquals(30, headcount0.getValue());
    assertEquals(Source.FORECAST, headcount0.getSource());

    final MagnitudePhoto headcount1 = headcounts.get(1);
    assertTrue(request.getDateTo().isEqual(headcount1.getDate()));
    assertEquals(PACKING, headcount1.getProcessName());
    assertEquals(20, headcount1.getValue());
    assertEquals(Source.SIMULATION, headcount1.getSource());
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void testGetEntitiesError(
      final Class<? extends Exception> exceptionClass,
      final String response
  ) {
    // GIVEN
    final TrajectoriesRequest request = TrajectoriesRequest.builder()
        .entityType(HEADCOUNT)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARBA01")
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .source(Source.FORECAST)
        .processName(List.of(PICKING, PACKING))
        .build();

    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + ENTITIES_URL, HEADCOUNT.getName().toLowerCase()))
        .withRequestBody(request.toString())
        .withStatusCode(NOT_FOUND.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response)
        .build();

    // WHEN - THEN
    assertThrows(exceptionClass, () -> client.getTrajectories(request));
  }

  @Test
  void testGetForecastMetadata() throws IOException {

    // GIVEN
    final ForecastMetadataRequest request = ForecastMetadataRequest.builder()
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .build();

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + format(GET_FORECAST_METADATA_URL, FBM_WMS_OUTBOUND))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(
            getResourceAsString("forecast_metadata_response.json"))
        .build();


    //WHEN
    final List<Metadata> forecastMetadata =
        client.getForecastMetadata(FBM_WMS_OUTBOUND, request);

    //THEN
    assertNotNull(forecastMetadata);
    assertEquals(5, forecastMetadata.size());
    forecastMetadataEqualTo(forecastMetadata.get(0),
        "mono_order_distribution", "58");
    forecastMetadataEqualTo(forecastMetadata.get(1),
        "multi_order_distribution", "23");
    forecastMetadataEqualTo(forecastMetadata.get(2),
        "multi_batch_distribution", "72");
    forecastMetadataEqualTo(forecastMetadata.get(3),
        "warehouse_id", "ARBA01");
    forecastMetadataEqualTo(forecastMetadata.get(4),
        "week", "48-2020");
  }

  @Test
  void testCreateForecastMetadataParams() {
    // GIVEN
    final ForecastMetadataRequest request = ForecastMetadataRequest.builder()
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .build();
    // WHEN
    Map<String, String> forecastMetadataParams = client.createForecastMetadataParams(request);
    // THEN
    assertEquals("ARTW01", forecastMetadataParams.get("warehouse_id"));
    assertNotNull(forecastMetadataParams.get("date_from"));
    assertNotNull(forecastMetadataParams.get("date_to"));
  }

  private void forecastMetadataEqualTo(final Metadata output,
                                       final String key,
                                       final String value) {
    assertEquals(key, output.getKey());
    assertEquals(value, output.getValue());
  }

  @Test
  void testGetProductivity() throws JSONException {

    // Given
    final ProductivityRequest request = ProductivityRequest.builder()
        .entityType(HEADCOUNT)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARTW01")
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .source(Source.FORECAST)
        .processName(List.of(PICKING, PACKING, PACKING_WALL))
        .abilityLevel(List.of(1, 2))
        .build();


    final JSONArray apiResponse = new JSONArray()
        .put(new JSONObject()
            .put("date", request.getDateFrom().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "picking")
            .put(VALUE_FIELD, "30")
            .put("source", "forecast")
            .put("metric_unit", "minutes")
            .put("ability_level", 1)
        )
        .put(new JSONObject()
            .put("date", request.getDateTo().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "packing")
            .put(VALUE_FIELD, "20")
            .put("source", "simulation")
            .put("metric_unit", "percentage")
            .put("ability_level", 2)
        );
    mockPostProductivity(apiResponse);

    // When
    final List<Productivity> productivities = client.getProductivity(request);

    // Then
    assertEquals(2, productivities.size());

    final Productivity productivity1 = productivities.get(0);
    assertTrue(request.getDateFrom().isEqual(productivity1.getDate()));
    assertEquals(PICKING, productivity1.getProcessName());
    assertEquals(30, productivity1.getValue());
    assertEquals(Source.FORECAST, productivity1.getSource());
    assertEquals(1, productivity1.getAbilityLevel());

    final Productivity productivity2 = productivities.get(1);
    assertTrue(request.getDateTo().isEqual(productivity2.getDate()));
    assertEquals(PACKING, productivity2.getProcessName());
    assertEquals(20, productivity2.getValue());
    assertEquals(Source.SIMULATION, productivity2.getSource());
    assertEquals(2, productivity2.getAbilityLevel());
  }

  @Test
  void testGetPerformedProcessing() throws JSONException {
    // Given
    ZonedDateTime now = DateUtils.getCurrentUtcDateTime();

    final TrajectoriesRequest request = TrajectoriesRequest.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARTW01")
        .dateFrom(now)
        .dateTo(now.plusDays(1))
        .source(Source.FORECAST)
        .processName(List.of(WAVING))
        .build();


    final JSONArray apiResponse = new JSONArray()
        .put(new JSONObject()
            .put("date", request.getDateFrom().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "waving")
            .put(VALUE_FIELD, "30")
            .put("source", "forecast")
            .put("metric_unit", "minutes")
            .put(TYPE_FIELD, "performed_processing")
        )
        .put(new JSONObject()
            .put("date", request.getDateTo().format(ISO_OFFSET_DATE_TIME))
            .put("workflow", "fbm-wms-outbound")
            .put("process_name", "waving")
            .put(VALUE_FIELD, "20")
            .put("source", "forecast")
            .put("metric_unit", "percentage")
            .put(TYPE_FIELD, "performed_processing")
        );

    mockGetPerformedProcessing(apiResponse);

    // When
    final List<MagnitudePhoto> targetBacklog = client.getPerformedProcessing(request);

    // Then
    assertEquals(2, targetBacklog.size());

    final MagnitudePhoto target1 = targetBacklog.get(0);
    assertEquals(request.getDateFrom(), target1.getDate());
    assertEquals(WAVING, target1.getProcessName());
    assertEquals(30, target1.getValue());
    assertEquals(Source.FORECAST, target1.getSource());

    final MagnitudePhoto target2 = targetBacklog.get(1);
    assertEquals(request.getDateTo(), target2.getDate());
    assertEquals(WAVING, target2.getProcessName());
    assertEquals(20, target2.getValue());
    assertEquals(Source.FORECAST, target2.getSource());
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void testGetPerformedProcessingError(
      final Class<Throwable> exceptionClass,
      final String response) {

    // GIVEN
    ZonedDateTime now = DateUtils.getCurrentUtcDateTime();

    final TrajectoriesRequest request = TrajectoriesRequest.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARTW01")
        .dateFrom(now)
        .dateTo(now.plusDays(1))
        .source(Source.FORECAST)
        .processName(List.of(WAVING))
        .build();

    MockResponse.builder()
        .withMethod(GET)
        .withURL(format(BASE_URL + ENTITIES_URL, PERFORMED_PROCESSING))
        .withStatusCode(NOT_FOUND.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response)
        .build();

    // WHEN - THEN
    assertThrows(exceptionClass, () -> client.getPerformedProcessing(request));
  }

  @Test
  void testCreateEntityParams() {
    // GIVEN
    final TrajectoriesRequest request = TrajectoriesRequest.builder()
        .entityType(HEADCOUNT)
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId("ARTW01")
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .source(Source.FORECAST)
        .processName(List.of(PICKING, PACKING))
        .build();
    // WHEN
    Map<String, String> entityParams = client.createEntityParams(request);
    // THEN
    assertEquals("picking,packing", entityParams.get("process_name"));
    assertNull(entityParams.get("source"));
    assertEquals("ARTW01", entityParams.get("warehouse_id"));
    assertNotNull(entityParams.get("date_from"));
    assertNotNull(entityParams.get("date_to"));
  }

  @Test
  void testRunProjectionOk() throws JSONException {
    // GIVEN
    final ProjectionRequest request = ProjectionRequest.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .type(ProjectionType.CPT)
        .processName(List.of(PICKING, PACKING))
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .backlog(List.of(
            new Backlog(parse("2020-09-29T10:00:00Z"), 100),
            new Backlog(parse("2020-09-29T11:00:00Z"), 200),
            new Backlog(parse("2020-09-29T12:00:00Z"), 300)
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
        .withURL(format(BASE_URL + RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND, "cpts"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(apiResponse.toString())
        .build();

    // When
    final List<ProjectionResult> projections = client.runProjection(request);

    // Then
    assertEquals(3, projections.size());

    final ProjectionResult cpt1 = projections.get(0);
    assertEquals(parse("2020-09-29T08:00:00Z", ISO_OFFSET_DATE_TIME),
        cpt1.getProjectedEndDate());
    assertEquals(0, cpt1.getRemainingQuantity());

    final ProjectionResult cpt2 = projections.get(1);
    assertEquals(parse("2020-09-29T10:00:00Z", ISO_OFFSET_DATE_TIME),
        cpt2.getProjectedEndDate());
    assertEquals(0, cpt2.getRemainingQuantity());

    final ProjectionResult cpt3 = projections.get(2);
    assertEquals(parse("2020-09-29T14:00:00Z", ISO_OFFSET_DATE_TIME),
        cpt3.getProjectedEndDate());
    assertEquals(70, cpt3.getRemainingQuantity());
  }

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void testRunProjectionError(
      final Class<? extends Exception> exceptionClass,
      final String response
  ) {
    // GIVEN
    final ProjectionRequest request = ProjectionRequest.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .type(ProjectionType.CPT)
        .processName(List.of(PICKING, PACKING))
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .backlog(List.of(
            new Backlog(parse("2020-09-29T10:00:00Z"), 100),
            new Backlog(parse("2020-09-29T11:00:00Z"), 200),
            new Backlog(parse("2020-09-29T12:00:00Z"), 300)
        ))
        .build();

    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND, "cpts"))
        .withStatusCode(NOT_FOUND.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response)
        .build();

    // When - Then
    assertThrows(exceptionClass, () -> client.runProjection(request));
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
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(apiResponse.toString())
        .build();


    // When
    final List<ProjectionResult> simulations = client.runSimulation(request);

    // Then
    assertEquals(4, simulations.size());

    final ProjectionResult sim1 = simulations.get(0);
    assertEquals(ZonedDateTime.parse("2020-07-27T11:00:00Z", ISO_OFFSET_DATE_TIME),
        sim1.getDate());
    assertEquals(ZonedDateTime.parse("2020-07-27T10:00:00Z", ISO_OFFSET_DATE_TIME),
        sim1.getProjectedEndDate());
    assertEquals(parse("2020-07-27T09:00:00Z", ISO_OFFSET_DATE_TIME),
        sim1.getSimulatedEndDate());
    assertEquals(1000, sim1.getRemainingQuantity());

    final ProjectionResult sim2 = simulations.get(1);
    assertEquals(ZonedDateTime.parse("2020-07-27T12:00:00Z", ISO_OFFSET_DATE_TIME),
        sim2.getDate());
    assertEquals(ZonedDateTime.parse("2020-07-27T10:40:00Z", ISO_OFFSET_DATE_TIME),
        sim2.getProjectedEndDate());
    assertEquals(parse("2020-07-27T09:30:00Z", ISO_OFFSET_DATE_TIME),
        sim2.getSimulatedEndDate());
    assertEquals(5000, sim2.getRemainingQuantity());

    final ProjectionResult sim3 = simulations.get(2);
    assertEquals(ZonedDateTime.parse("2020-07-27T03:00:00Z", ISO_OFFSET_DATE_TIME),
        sim3.getDate());
    assertEquals(ZonedDateTime.parse("2020-07-27T02:15:00Z", ISO_OFFSET_DATE_TIME),
        sim3.getProjectedEndDate());
    assertEquals(parse("2020-07-27T01:40:00Z", ISO_OFFSET_DATE_TIME),
        sim3.getSimulatedEndDate());
    assertEquals(2100, sim3.getRemainingQuantity());

    final ProjectionResult sim4 = simulations.get(3);
    assertEquals(ZonedDateTime.parse("2020-07-27T05:00:00Z", ISO_OFFSET_DATE_TIME),
        sim4.getDate());
    assertEquals(ZonedDateTime.parse("2020-07-27T06:00:00Z", ISO_OFFSET_DATE_TIME),
        sim4.getProjectedEndDate());
    assertEquals(parse("2020-07-27T05:10:00Z", ISO_OFFSET_DATE_TIME),
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
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(apiResponse.toString())
        .build();

    // When
    final List<ProjectionResult> simulations = client.saveSimulation(request);

    // Then
    assertEquals(4, simulations.size());

    final ProjectionResult sim1 = simulations.get(0);
    assertNull(sim1.getSimulatedEndDate());
    assertEquals(parse("2020-07-27T11:00:00Z", ISO_OFFSET_DATE_TIME),
        sim1.getDate());
    assertEquals(parse("2020-07-27T10:00:00Z", ISO_OFFSET_DATE_TIME),
        sim1.getProjectedEndDate());
    assertEquals(1000, sim1.getRemainingQuantity());

    final ProjectionResult sim2 = simulations.get(1);
    assertNull(sim2.getSimulatedEndDate());
    assertEquals(parse("2020-07-27T12:00:00Z", ISO_OFFSET_DATE_TIME),
        sim2.getDate());
    assertEquals(parse("2020-07-27T10:40:00Z", ISO_OFFSET_DATE_TIME),
        sim2.getProjectedEndDate());
    assertEquals(5000, sim2.getRemainingQuantity());

    final ProjectionResult sim3 = simulations.get(2);
    assertNull(sim3.getSimulatedEndDate());
    assertEquals(parse("2020-07-27T03:00:00Z", ISO_OFFSET_DATE_TIME),
        sim3.getDate());
    assertEquals(parse("2020-07-27T02:15:00Z", ISO_OFFSET_DATE_TIME),
        sim3.getProjectedEndDate());
    assertEquals(2100, sim3.getRemainingQuantity());

    final ProjectionResult sim4 = simulations.get(3);
    assertNull(sim4.getSimulatedEndDate());
    assertEquals(parse("2020-07-27T05:00:00Z", ISO_OFFSET_DATE_TIME),
        sim4.getDate());
    assertEquals(parse("2020-07-27T06:00:00Z", ISO_OFFSET_DATE_TIME),
        sim4.getProjectedEndDate());
    assertEquals(1700, sim4.getRemainingQuantity());
  }

  @Test
  void testGetConfigurationOk() throws JSONException {
    // GIVEN
    final ConfigurationRequest request = ConfigurationRequest.builder()
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .key("estimated_delivery_time")
        .build();

    final JSONObject response = new JSONObject()
        .put(VALUE_FIELD, "60")
        .put("metric_unit", "minutes");

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + CONFIGURATION_URL)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();

    // WHEN
    final Optional<ConfigurationResponse> configurationResponse =
        client.getConfiguration(request);
    // THEN
    assertNotNull(configurationResponse);
    assertEquals(
        Optional.of(60L),
        configurationResponse.map(ConfigurationResponse::getValue)
    );
    assertEquals(
        Optional.of(MINUTES),
        configurationResponse.map(ConfigurationResponse::getMetricUnit)
    );
  }

  @Test
  void testGetPlanningDistributionOk() throws JSONException {
    // GIVEN
    final ZonedDateTime currentTime =
        now().withMinute(0).withSecond(0).withNano(0);
    final ZonedDateTime cpt1 = currentTime.plusHours(4);
    final ZonedDateTime cpt2 = currentTime.plusHours(5);
    final ZonedDateTime cpt3 = currentTime.plusHours(6);

    final PlanningDistributionRequest request = new PlanningDistributionRequest(
        WAREHOUSE_ID_ARTW01,
        FBM_WMS_OUTBOUND,
        currentTime,
        currentTime,
        currentTime.plusDays(1),
        true
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
        .withStatusCode(OK.value())
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

  @ParameterizedTest
  @MethodSource("errorResponseProvider")
  void testGetPlanningDistributionError(
      final Class<? extends Exception> exceptionClass,
      final String response
  ) {
    // GIVEN
    final ZonedDateTime currentTime = now().withMinute(0).withSecond(0).withNano(0);

    final PlanningDistributionRequest request = new PlanningDistributionRequest(
        WAREHOUSE_ID_ARTW01,
        FBM_WMS_OUTBOUND,
        currentTime,
        currentTime,
        currentTime.plusDays(1),
        true
    );

    MockResponse.builder()
        .withMethod(GET)
        .withURL(format(BASE_URL + PLANNING_DISTRIBUTION_URL, FBM_WMS_OUTBOUND.getName()))
        .withStatusCode(NOT_FOUND.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response)
        .build();

    // WHEN - THEN
    assertThrows(exceptionClass, () -> client.getPlanningDistribution(request));
  }

  @Test
  public void testGetBacklogProjection() throws IOException {
    // GIVEN
    final BacklogProjectionRequest request = BacklogProjectionRequest.builder()
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .dateFrom(now())
        .dateTo(now().plusDays(1))
        .workflow(FBM_WMS_OUTBOUND)
        .processName(List.of(WAVING, PICKING, PACKING))
        .currentBacklog(List.of(
            new CurrentBacklog(WAVING, 100),
            new CurrentBacklog(PICKING, 150),
            new CurrentBacklog(PACKING, 200)))
        .build();

    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND, "backlogs"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString("get_backlog_projection_api_response.json"))
        .build();

    // WHEN
    final List<BacklogProjectionResponse> response = client.getBacklogProjection(request);

    // THEN
    assertEquals(3, response.size());
    assertEquals(WAVING, response.get(0).getProcessName());
    assertEquals(PICKING, response.get(1).getProcessName());
    assertEquals(PACKING, response.get(2).getProcessName());

    final List<ProjectionValue> wavingValues = response.get(0).getValues();
    final List<ProjectionValue> pickingValues = response.get(1).getValues();
    final List<ProjectionValue> packingValues = response.get(2).getValues();

    assertEquals(3, wavingValues.size());
    assertEquals(ZonedDateTime.parse("2020-01-01T10:00:00Z"), wavingValues.get(0).getDate());
    assertEquals(1000, wavingValues.get(0).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T11:00:00Z"), wavingValues.get(1).getDate());
    assertEquals(2000, wavingValues.get(1).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T12:00:00Z"), wavingValues.get(2).getDate());
    assertEquals(3000, wavingValues.get(2).getQuantity());

    assertEquals(3, pickingValues.size());
    assertEquals(ZonedDateTime.parse("2020-01-01T10:00:00Z"), pickingValues.get(0).getDate());
    assertEquals(1100, pickingValues.get(0).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T11:00:00Z"), pickingValues.get(1).getDate());
    assertEquals(2100, pickingValues.get(1).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T12:00:00Z"), pickingValues.get(2).getDate());
    assertEquals(3100, pickingValues.get(2).getQuantity());

    assertEquals(3, packingValues.size());
    assertEquals(ZonedDateTime.parse("2020-01-01T10:00:00Z"), packingValues.get(0).getDate());
    assertEquals(1200, packingValues.get(0).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T11:00:00Z"), packingValues.get(1).getDate());
    assertEquals(2200, packingValues.get(1).getQuantity());
    assertEquals(ZonedDateTime.parse("2020-01-01T12:00:00Z"), packingValues.get(2).getDate());
    assertEquals(3200, packingValues.get(2).getQuantity());
  }

  @Test
  void testBacklogProjectionByArea() throws IOException {
    // GIVEN
    final var dateFrom = Instant.parse("2022-05-05T18:00:00Z");
    final var zonedDateFrom = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final var dateTo = Instant.parse("2022-05-05T20:00:00Z");
    final var zonedDateTo = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC);

    final var processes = List.of(WAVING, PICKING);

    final var backlog = List.of(
        new BacklogQuantityAtSla(WAVING, dateFrom.plus(1L, HOURS), 100),
        new BacklogQuantityAtSla(WAVING, dateFrom.plus(2L, HOURS), 150),
        new BacklogQuantityAtSla(PICKING, dateFrom.plus(1L, HOURS), 200)
    );

    final var planned = List.of(
        PlanningDistributionResponse.builder()
            .dateIn(zonedDateFrom)
            .dateOut(zonedDateTo)
            .metricUnit(UNITS)
            .total(321)
            .build(),
        PlanningDistributionResponse.builder()
            .dateIn(zonedDateFrom.plusHours(1L))
            .dateOut(zonedDateTo.plusHours(1L))
            .metricUnit(UNITS)
            .total(321)
            .build()
    );

    final var tph = List.of(
        MagnitudePhoto.builder()
            .date(zonedDateFrom)
            .workflow(FBM_WMS_OUTBOUND)
            .processName(WAVING)
            .value(300)
            .build(),
        MagnitudePhoto.builder()
            .date(zonedDateTo)
            .workflow(FBM_WMS_OUTBOUND)
            .processName(PICKING)
            .value(250)
            .build()
    );

    final var share = List.of(
        new BacklogAreaDistribution(PICKING, dateFrom, "BL-0", 0.10),
        new BacklogAreaDistribution(PICKING, dateFrom, "MZ-1", 0.75),
        new BacklogAreaDistribution(PICKING, dateFrom, "MZ-2", 0.15)
    );

    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND, "backlogs/grouped/area"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString("get_backlog_projection_by_area_response.json"))
        .build();

    // WHEN
    final var response = client.projectBacklogInAreas(dateFrom, dateTo, FBM_WMS_OUTBOUND, processes, backlog, planned, tph, share);

    // THEN
    assertNotNull(response);

    assertEquals(12, response.size());

    final var firstResult = response.get(0);
    assertEquals("BL-0", firstResult.getArea());
    assertEquals(PROCESSED, firstResult.getStatus());
    assertEquals(PICKING, firstResult.getProcess());
    assertEquals(43, firstResult.getQuantity());
    assertEquals(dateFrom, firstResult.getOperatingHour());

    final var lastResult = response.get(11);
    assertEquals("MZ-2", lastResult.getArea());
    assertEquals(CARRY_OVER, lastResult.getStatus());
    assertEquals(PICKING, lastResult.getProcess());
    assertEquals(45, lastResult.getQuantity());
    assertEquals(dateFrom.plusSeconds(60L * 60L), lastResult.getOperatingHour());
  }

  @Test
  void testSearchEntities() throws IOException {
    // Given
    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + ENTITIES_URL, "search"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString("search_entities_response.json"))
        .build();

    // When
    final Map<MagnitudeType, List<MagnitudePhoto>> entities = client.searchTrajectories(
        SearchTrajectoriesRequest.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .entityTypes(List.of(HEADCOUNT, PRODUCTIVITY))
            .processName(List.of(PICKING, PACKING))
            .entityFilters(Map.of(PRODUCTIVITY, Map.of("ability_level", List.of("1"))))
            .build());

    // Then
    assertEquals(2, entities.size());
    assertEquals(6, entities.get(HEADCOUNT).size());
    assertEquals(2, entities.get(PRODUCTIVITY).size());

    final MagnitudePhoto headcount = entities.get(HEADCOUNT).get(0);
    final Productivity productivity = (Productivity) entities.get(PRODUCTIVITY).get(0);

    assertEquals(FBM_WMS_OUTBOUND, headcount.getWorkflow());
    assertEquals(PACKING, headcount.getProcessName());
    assertEquals(Source.FORECAST, headcount.getSource());
    assertEquals("2021-01-10T00:00Z", headcount.getDate().toString());
    assertEquals(23, headcount.getValue());

    assertEquals(FBM_WMS_OUTBOUND, productivity.getWorkflow());
    assertEquals(PACKING, productivity.getProcessName());
    assertEquals(Source.FORECAST, productivity.getSource());
    assertEquals("2021-01-10T00:00Z", productivity.getDate().toString());
    assertEquals(50, productivity.getValue());
    assertEquals(1, productivity.getAbilityLevel());
  }

  private void mockPostEntity(final JSONArray response) {
    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + ENTITIES_URL, "headcount"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();
  }

  private void mockPostProductivity(final JSONArray response) {
    MockResponse.builder()
        .withMethod(POST)
        .withURL(format(BASE_URL + ENTITIES_URL, "productivity"))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();
  }

  private void mockGetPerformedProcessing(final JSONArray response) {
    MockResponse.builder()
        .withMethod(GET)
        .withURL(format(BASE_URL + ENTITIES_URL, PERFORMED_PROCESSING))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();
  }

  private SimulationRequest mockSimulationRequest() {
    return SimulationRequest
        .builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .processName(List.of(PACKING, PICKING))
        .dateFrom(parse("2020-07-27T09:00:00Z"))
        .dateTo(parse("2020-07-28T09:00:00Z"))
        .backlog(List.of(
            new QuantityByDate(parse("2020-07-27T11:00:00Z"), 15002),
            new QuantityByDate(parse("2020-07-27T12:00:00Z"), 1500)
        ))
        .simulations(List.of(
            new Simulation(PICKING, List.of(
                new SimulationEntity(HEADCOUNT, List.of(
                    new QuantityByDate(parse("2020-07-27T10:00:00Z"), 32),
                    new QuantityByDate(parse("2020-07-27T14:00:00Z"), 32)
                )),
                new SimulationEntity(PRODUCTIVITY, List.of(
                    new QuantityByDate(parse("2020-07-27T10:00:00Z"), 40),
                    new QuantityByDate(parse("2020-07-27T14:00:00Z"), 60)
                )))
            ),
            new Simulation(PACKING, List.of(
                new SimulationEntity(HEADCOUNT, List.of(
                    new QuantityByDate(parse("2020-07-27T10:00:00Z"), 32),
                    new QuantityByDate(parse("2020-07-27T14:00:00Z"), 32)
                )),
                new SimulationEntity(PRODUCTIVITY, List.of(
                    new QuantityByDate(parse("2020-07-27T10:00:00Z"), 40),
                    new QuantityByDate(parse("2020-07-27T14:00:00Z"), 60)
                )))
            )))
        .build();
  }

  @Test
  public void saveShareDistributionTest() throws JSONException {

    //GIVEN
    List<ShareDistribution> shareDistributionList = List.of(ShareDistribution.builder().build());

    final JSONObject apiResponse = new JSONObject()
        .put("warehouse_id", "ARTW01")
        .put("response", "Successfully")
        .put("quantity_save", "0");

    MockResponse.builder()
        .withMethod(POST)
        .withURL(BASE_URL + format(UNITS_DISTRIBUTION, FBM_WMS_OUTBOUND))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(apiResponse.toString())
        .build();

    //WHEN
    SaveUnitsResponse response = client.saveShareDistribution(shareDistributionList, FBM_WMS_OUTBOUND);

    //THEN
    assertNotNull(response);
  }

  @Test
  public void testGetShareDistribution() throws JSONException {

    //GIVEN
    final JSONArray apiResponse = new JSONArray()
        .put(new JSONObject()
            .put("id", "34")
            .put("logistic_center_id", "ARBA01")
            .put("date", "2020-07-27T09:00:00Z")
            .put("process_name", "PICKING")
            .put("area", "MZ-05")
            .put("quantity", "0.5")
            .put("quantity_metric_unit", "PERCENTAGE")
        );
    GetShareDistributionInput request = new GetShareDistributionInput(now(), now().plusDays(1), "ARBA01");

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + format(UNITS_DISTRIBUTION, FBM_WMS_OUTBOUND))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(apiResponse.toString())
        .build();

    //WHEN
    List<GetUnitsResponse> response = client.getShareDistribution(request, FBM_WMS_OUTBOUND);

    //THEN
    assertNotNull(response);
  }

  @Test
  public void saveSimulationsTest() {
    //GIVEN
    MockResponse.builder()
        .withMethod(POST)
        .withURL(BASE_URL + format(BASE_SIMULATIONS_URL, FBM_WMS_OUTBOUND) + "/deferral/save")
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody("")
        .build();

    client.deferralSaveSimulation(new SaveSimulationsRequest(FBM_WMS_OUTBOUND, WAREHOUSE_ID_ARTW01, emptyList(), 1L));

  }

    @Test
    public void deferralStatusProjectionTest() throws JSONException {

        //GIVEN
        final JSONArray apiResponse = new JSONArray()
            .put(new JSONObject()
                .put("sla", "2022-07-27T09:00:00Z")
                .put("deferred_at", "2022-07-27T08:00:00Z")
                .put("deferred_units", "5")
                .put("deferral_status", "deferred_cap_max")
            );

        MockResponse.builder()
            .withMethod(POST)
            .withURL(BASE_URL + format(RUN_PROJECTIONS_URL, FBM_WMS_OUTBOUND, "cpts/deferral_time"))
            .withStatusCode(OK.value())
            .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
            .withResponseBody(apiResponse.toString())
            .build();

        List<DeferralProjectionStatus> response = client.getDeferralProjectionStatus(
            Instant.parse("2022-07-27T00:00:00Z"),
            Instant.parse("2022-07-27T23:00:00Z"),
            FBM_WMS_OUTBOUND,
            List.of(PICKING, PACKING),
            List.of(new BacklogQuantity(Instant.parse("2022-07-27T09:00:00Z"), 3000)),
            WAREHOUSE_ID_ARTW01,
            "America/Buenos_Aires",
            true,
            emptyList());

        assertNotNull(response);
        assertEquals(1, response.size());

    }

  @Nested
  @DisplayName("Test save deviation")
  class SaveOutboundDeviationTest {

    @Test
    void testSaveDeviationOk() throws Exception {
      // Given
      final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
          .workflow(FBM_WMS_OUTBOUND)
          .warehouseId(WAREHOUSE_ID_ARTW01)
          .dateFrom(now())
          .dateTo(now().plusDays(1))
          .value(5.9)
          .userId(USER_ID)
          .build();

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/save", FBM_WMS_OUTBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", OK.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse = client.saveDeviation(saveDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(200, deviationResponse.getStatus());
    }

    @Test
    void testSaveDeviationError() throws Exception {
      // Given
      final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
          .workflow(FBM_WMS_OUTBOUND)
          .warehouseId(WAREHOUSE_ID_ARTW01)
          .dateFrom(now())
          .dateTo(now().plusDays(1))
          .value(5.9)
          .userId(USER_ID)
          .build();

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/save", FBM_WMS_OUTBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", BAD_REQUEST.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse = client.saveDeviation(saveDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(400, deviationResponse.getStatus());
    }
  }

  @Nested
  @DisplayName("Test new save deviation")
  class NewSaveOutboundDeviationTest {

    @Test
    void testNewSaveDeviationOk() throws Exception {
      // Given
      final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
          .workflow(FBM_WMS_INBOUND)
          .warehouseId(WAREHOUSE_ID_ARTW01)
          .dateFrom(now())
          .dateTo(now().plusDays(1))
          .type(DeviationType.UNITS)
          .value(5.9)
          .userId(USER_ID)
          .build();

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/" + DeviationType.UNITS.getName() + "/save", FBM_WMS_INBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", OK.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse = client.newSaveDeviation(saveDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(200, deviationResponse.getStatus());
    }

    @Test
    void testNewSaveDeviationError() throws Exception {
      // Given
      final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
          .workflow(FBM_WMS_INBOUND)
          .warehouseId(WAREHOUSE_ID_ARTW01)
          .dateFrom(now())
          .dateTo(now().plusDays(1))
          .type(DeviationType.UNITS)
          .value(5.9)
          .userId(USER_ID)
          .build();

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/" + DeviationType.UNITS.getName() + "/save", FBM_WMS_INBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", BAD_REQUEST.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse = client.newSaveDeviation(saveDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(400, deviationResponse.getStatus());
    }
  }

  @Nested
  @DisplayName("Test disable deviation")
  class DisableDeviationTest {

    @Test
    void testDisableDeviationOk() throws Exception {
      // Given
      final DisableDeviationInput disableDeviationInput =
          new DisableDeviationInput(WAREHOUSE_ID_ARTW01, FBM_WMS_OUTBOUND);

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/disable", FBM_WMS_OUTBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", OK.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse =
          client.disableDeviation(disableDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(200, deviationResponse.getStatus());
    }

    @Test
    void testDisableDeviationError() throws Exception {
      // Given
      final DisableDeviationInput disableDeviationInput =
          new DisableDeviationInput(WAREHOUSE_ID_ARTW01, FBM_WMS_OUTBOUND);

      MockResponse.builder()
          .withMethod(POST)
          .withURL(format(BASE_URL + DEVIATION_URL + "/disable", FBM_WMS_OUTBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", BAD_REQUEST.value())
              .toString())
          .build();

      // When
      final DeviationResponse deviationResponse =
          client.disableDeviation(disableDeviationInput);

      // Then
      assertNotNull(deviationResponse);
      assertEquals(400, deviationResponse.getStatus());
    }
  }

  @Nested
  @DisplayName("Test get deviation")
  class GetDeviationTest {

    @Test
    void testGetDeviationOk() throws Exception {
      // Given
      MockResponse.builder()
          .withMethod(GET)
          .withURL(format(BASE_URL + DEVIATION_URL, FBM_WMS_OUTBOUND))
          .withStatusCode(OK.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(getResourceAsString("get_deviation_response.json"))
          .build();

      // When
      final GetDeviationResponse getDeviationResponse =
          client.getDeviation(FBM_WMS_OUTBOUND, WAREHOUSE_ID_ARTW01, A_DATE);

      // Then
      assertNotNull(getDeviationResponse);
      assertEquals(ZonedDateTime.of(2021, 1, 21, 15, 0, 0, 0, ZoneId.of("Z")),
          getDeviationResponse.getDateFrom());
      assertEquals(ZonedDateTime.of(2021, 1, 21, 17, 0, 0, 0, ZoneId.of("Z")),
          getDeviationResponse.getDateTo());
      assertEquals(5.8, getDeviationResponse.getValue());
      assertEquals(PERCENTAGE, getDeviationResponse.getMetricUnit());
    }

      @Test
      void testGetListOfDeviationOk() throws Exception {
          // Given
          final Instant currentTime = Instant.parse("2022-01-11T15:00:00Z");
          final JSONArray response = new JSONArray()
                  .put(new JSONObject()
                          .put("workflow", FBM_WMS_INBOUND.getName())
                          .put(TYPE_FIELD, "minutes")
                          .put("type", "UNITS")
                          .put("date_from", currentTime)
                          .put("date_to", currentTime.plus(5, HOURS))
                          .put(VALUE_FIELD, 5.8)
                          .put("metric_unit", "percentage")
                  )
                  .put(new JSONObject()
                          .put("workflow", FBM_WMS_INBOUND.getName())
                          .put(TYPE_FIELD, "minutes")
                          .put("type", "MINUTES")
                          .put("date_from", currentTime.plus(1, HOURS))
                          .put("date_to", currentTime.plus(6, HOURS))
                          .put(VALUE_FIELD, 3.6)
                          .put("metric_unit", "percentage")
                  );

          MockResponse.builder()
                  .withMethod(GET)
                  .withURL(format(BASE_URL + ACTIVE_DEVIATIONS_URL, FBM_WMS_INBOUND))
                  .withStatusCode(OK.value())
                  .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                  .withResponseBody(response.toString())
                  .build();

          var workflow = Set.of(FBM_WMS_INBOUND);
          // When
          final List<Deviation> getActiveDeviations =
                  client.getActiveDeviations(workflow, WAREHOUSE_ID_ARTW01, A_DATE.toInstant());
          final Deviation firstElementInRepsonse = getActiveDeviations.get(0);
          // Then

          assertNotNull(getActiveDeviations);
          assertEquals(
              Instant.parse("2022-01-11T15:00:00Z"),
                  firstElementInRepsonse.getDateFrom());
          assertEquals(
              Instant.parse("2022-01-11T20:00:00Z"),
                  firstElementInRepsonse.getDateTo());
          assertEquals(5.8, firstElementInRepsonse.getValue());
      }

    @Test
    void testGetDeviationWhenNotExistDeviation() throws Exception {
      // Given
      MockResponse.builder()
          .withMethod(GET)
          .withURL(format(BASE_URL + DEVIATION_URL, FBM_WMS_OUTBOUND))
          .withStatusCode(BAD_REQUEST.value())
          .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
          .withResponseBody(new JSONObject()
              .put("status", "NOT_FOUND")
              .put("message",
                  "Entity CurrentForecastDeviation with id ARTW01 was not found")
              .put("error", "entity_not_found")
              .toString())
          .build();

      // Then
      assertThrows(ClientException.class,
          () -> client.getDeviation(FBM_WMS_OUTBOUND, WAREHOUSE_ID_ARTW01, A_DATE));
    }
  }
}
