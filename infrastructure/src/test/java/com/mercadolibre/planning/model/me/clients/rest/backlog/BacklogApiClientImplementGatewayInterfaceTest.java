package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.controller.backlog.exception.BacklogNotRespondingException;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.restclient.MockResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BacklogApiClientImplementGatewayInterfaceTest extends BaseClientTest {
  private static final String URL = "/backlogs/logistic_centers/%s/backlogs";

  private static final String URL_PHOTO = "/backlogs/logistic_centers/%s/photos";

  private static final String URL_LAST_PHOTO = "/backlogs/logistic_centers/%s/photos/last";

  private static final String QUERY_PARAMS =
      "workflows=outbound_orders&date_from=2021-01-01T00:00&date_to=2021-01-02T05:00";

  private static final String WORKFLOW = "outbound_order";

  private static final Instant DATE_CURRENT = Instant.parse("2021-08-12T02:00:00Z");

  private static final Instant DATE_FROM = Instant.parse("2021-08-12T01:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2021-08-12T05:00:00Z");

  private BacklogApiClientImplement client;

  private static Stream<Arguments> testParameters() {
    // String, BacklogRequest request
    return Stream.of(
        arguments(
            "",
            BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID_ARTW01)
                .build()),
        arguments(
            "?workflows=orders,withdrawal&"
                + "processes=rtw,picking&"
                + "date_from=2021-08-12T01:00:00Z&"
                + "date_to=2021-08-12T05:00:00Z&"
                + "group_by=workflow,process,area",
            BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID_ARTW01)
                .workflows(of("orders", "withdrawal"))
                .processes(of("rtw", "picking"))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .groupingFields(of(
                    "workflow", "process", "area"
                ))
                .build()

        ));
  }

  @BeforeEach
  void setUp() throws Exception {
    client = new BacklogApiClientImplement(getRestTestClient());
  }

  @AfterEach
  void tearDown() {
    cleanMocks();
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  void testGetBacklogOK(String query, BacklogRequest request) throws JSONException {
    // GIVEN
    final String url = String.format(URL, WAREHOUSE_ID_ARTW01) + query;
    mockSuccessfulResponse(url, buildResponse());

    // WHEN
    final List<Consolidation> result = client.getBacklog(request);

    // THEN
    assertEquals(3, result.size());

    final Consolidation firstConsolidation = result.get(0);
    assertEquals(1255, firstConsolidation.getTotal());
    assertEquals(DATE_FROM, firstConsolidation.getDate());

    final Map<String, String> keys = firstConsolidation.getKeys();
    assertEquals("wms-outbound", keys.get("workflow"));
    assertEquals("picking", keys.get("process"));
    assertEquals("MZ-0", keys.get("area"));
    assertEquals("2021-01-02T00:00", keys.get("date_out"));
  }

  @Test
  void testGetCurrentBacklogOK() throws JSONException {
    // GIVEN
    final String url = String.format(URL, WAREHOUSE_ID_ARTW01) + "/current";
    mockSuccessfulResponse(url, buildResponse());

    // WHEN
    final List<Consolidation> result = client.getCurrentBacklog(
        WAREHOUSE_ID_ARTW01,
        null,
        null,
        null,
        null,
        null
    );

    // THEN
    assertEquals(3, result.size());

    final Consolidation firstConsolidation = result.get(0);
    assertEquals(1255, firstConsolidation.getTotal());
    assertEquals(DATE_FROM, firstConsolidation.getDate());

    final Map<String, String> keys = firstConsolidation.getKeys();
    assertEquals("wms-outbound", keys.get("workflow"));
    assertEquals("picking", keys.get("process"));
    assertEquals("MZ-0", keys.get("area"));
    assertEquals("2021-01-02T00:00", keys.get("date_out"));
  }

  @Test
  void testGetCurrentBacklogWithDateInOK() throws JSONException {
    // GIVEN
    final String url = String.format(URL, WAREHOUSE_ID_ARTW01) + "/current";
    mockSuccessfulResponse(url, buildResponse());

    // WHEN
    final List<Consolidation> result = client.getCurrentBacklog(
        new BacklogCurrentRequest(WAREHOUSE_ID_ARTW01)
    );

    // THEN
    assertEquals(3, result.size());

    final Consolidation firstConsolidation = result.get(0);
    assertEquals(1255, firstConsolidation.getTotal());
    assertEquals(DATE_FROM, firstConsolidation.getDate());

    final Map<String, String> keys = firstConsolidation.getKeys();
    assertEquals("wms-outbound", keys.get("workflow"));
    assertEquals("picking", keys.get("process"));
    assertEquals("MZ-0", keys.get("area"));
    assertEquals("2021-01-02T00:00", keys.get("date_out"));
  }

  @Test
  void testGetLastPhoto() throws JSONException {
    // GIVEN
    final String url = String.format(URL_LAST_PHOTO, WAREHOUSE_ID_ARTW01);
    var response = buildResponseLastPhoto();

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + url)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();

    final Instant dateFrom = Instant.parse("2022-06-22T00:00:00Z");
    final Instant dateTo = Instant.parse("2022-06-24T00:00:00Z");

    // WHEN
    final Photo photo = client.getLastPhoto(
        new BacklogLastPhotoRequest(
            WAREHOUSE_ID_ARTW01,
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            Set.of(Step.TO_PICK),
            dateFrom,
            dateTo,
            dateFrom,
            dateTo,
            Set.of(BacklogGrouper.STEP),
            dateTo
        )
    );

    //THEN
    assertEquals(Instant.parse("2022-06-21T12:00:00Z"), photo.getTakenOn());
    assertEquals("to_pick", photo.getGroups().get(0).getGroupValue(BacklogGrouper.STEP).get());
    assertEquals(12, photo.getGroups().get(0).getTotal());
  }

  @Test
  void testGetPhotos() throws JSONException {
    // GIVEN
    final String url = String.format(URL_PHOTO, WAREHOUSE_ID_ARTW01);
    mockSuccessfulResponse(url, buildResponsePhotos());

    final Instant dateFrom = Instant.parse("2022-06-22T00:00:00Z");
    final Instant dateTo = Instant.parse("2022-06-24T00:00:00Z");

    // WHEN
    final List<Photo> photos = client.getPhotos(
        new BacklogPhotosRequest(
            WAREHOUSE_ID_ARTW01,
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            Set.of(Step.TO_PICK),
            dateFrom,
            dateTo,
            dateFrom,
            dateTo,
            Set.of(BacklogGrouper.STEP),
            dateFrom,
            dateTo
        )
    );

    final Photo photo = photos.get(0);

    assertEquals(Instant.parse("2022-06-21T12:00:00Z"), photo.getTakenOn());
    assertEquals("to_pick", photo.getGroups().get(0).getGroupValue(BacklogGrouper.STEP).get());
    assertEquals(12, photo.getGroups().get(0).getTotal());
  }

  @Test
  void testGetPhotosCached() throws JSONException {
    // GIVEN
    final String url = String.format(URL_PHOTO, WAREHOUSE_ID_ARTW01);
    mockSuccessfulResponse(url, buildResponsePhotos());

    final Instant dateFrom = Instant.parse("2022-06-22T00:00:00Z");
    final Instant dateTo = Instant.parse("2022-06-24T00:00:00Z");

    // WHEN
    final List<Photo> photos = client.getPhotosCached(
        new BacklogPhotosRequest(
            WAREHOUSE_ID_ARTW01,
            Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
            Set.of(Step.TO_PICK),
            dateFrom,
            dateTo,
            dateFrom,
            dateTo,
            Set.of(BacklogGrouper.STEP),
            dateFrom,
            dateTo
        )
    );

    final Photo photo = photos.get(0);

    assertEquals(Instant.parse("2022-06-21T12:00:00Z"), photo.getTakenOn());
    assertEquals("to_pick", photo.getGroups().get(0).getGroupValue(BacklogGrouper.STEP).get());
    assertEquals(12, photo.getGroups().get(0).getTotal());
  }

  @Test
  void testGetBacklogErr() {
    // GIVEN
    mockErroneousResponse();
    BacklogRequest request = new BacklogRequest(
        DATE_CURRENT,
        WAREHOUSE_ID_ARTW01,
        of(WORKFLOW),
        of(),
        of(),
        of(),
        DATE_FROM,
        DATE_TO,
        DATE_CURRENT,
        DATE_CURRENT.plus(24, ChronoUnit.HOURS)
    );

    // WHEN
    assertThrows(BacklogNotRespondingException.class, () ->
        client.getBacklog(request)
    );
  }

  private void mockSuccessfulResponse(final String url, final JSONArray response) {
    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + url)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(response.toString())
        .build();
  }

  private JSONArray buildResponse() throws JSONException {
    Map<String, String> mockedValues = Map.of(
        "workflow", "wms-outbound",
        "process", "picking",
        "date_out", "2021-01-02T00:00",
        "area", "MZ-0");

    return new JSONArray()
        .put(
            new JSONObject()
                .put("date", "2021-08-12T01:00:00Z")
                .put("total", 1255)
                .put("keys", new JSONObject(mockedValues)))
        .put(
            new JSONObject()
                .put("date", "2021-08-12T02:00:00Z")
                .put("total", 255)
                .put("keys", new JSONObject(mockedValues)))
        .put(
            new JSONObject()
                .put("date", "2021-08-12T03:00:00Z")
                .put("total", 300)
                .put("keys", new JSONObject(mockedValues)));
  }

  private JSONObject buildResponseLastPhoto() throws JSONException {
    return new JSONObject()
        .put("taken_on", "2022-06-21T12:00:00Z")
        .put("groups", new JSONArray()
            .put(new JSONObject()
                .put("key", new JSONObject(Map.of("step", "to_pick")))
                .put("total", 12)
            )

        );

  }

  private JSONArray buildResponsePhotos() throws JSONException {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("taken_on", "2022-06-21T12:00:00Z")
                .put("groups", new JSONArray()
                    .put(new JSONObject()
                        .put("key", new JSONObject(Map.of("step", "to_pick")))
                        .put("total", 12)
                    )
                )
        );
  }

  private void mockErroneousResponse() {
    final String url = String.format(URL, WAREHOUSE_ID_ARTW01) + "?" + QUERY_PARAMS;

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL + url)
        .withStatusCode(INTERNAL_SERVER_ERROR.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .build();
  }
}
