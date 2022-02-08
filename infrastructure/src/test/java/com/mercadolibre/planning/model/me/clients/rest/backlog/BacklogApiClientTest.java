package com.mercadolibre.planning.model.me.clients.rest.backlog;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.restclient.MockResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

class BacklogApiClientTest extends BaseClientTest {
    private static final String URL = "/backlogs/logistic_centers/%s/backlogs";

    private static final String QUERY_PARAMS =
            "workflows=outbound_orders&date_from=2021-01-01T00:00&date_to=2021-01-02T05:00";

    private static final String WORKFLOW = "outbound_order";

    private static final Instant DATE_CURRENT = Instant.parse("2021-08-12T02:00:00Z");

    private static final Instant DATE_FROM = Instant.parse("2021-08-12T01:00:00Z");

    private static final Instant DATE_TO = Instant.parse("2021-08-12T05:00:00Z");

    private BacklogApiClient client;

    private static Stream<Arguments> testParameters() {
        // String, BacklogRequest request
        return Stream.of(
                arguments(
                        "",
                        BacklogRequest.builder()
                                .warehouseId(WAREHOUSE_ID)
                                .build()),
                arguments(
                        "?workflows=orders,withdrawal&"
                                + "processes=rtw,picking&"
                                + "date_from=2021-08-12T01:00:00Z&"
                                + "date_to=2021-08-12T05:00:00Z&"
                                + "group_by=workflow,process,area",
                        BacklogRequest.builder()
                                .warehouseId(WAREHOUSE_ID)
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
        client = new BacklogApiClient(getRestTestClient());
    }

    @AfterEach
    void tearDown() {
        cleanMocks();
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    void testGetBacklogOK(String query, BacklogRequest request) throws JSONException {
        // GIVEN
        mockSuccessfulResponse(query);

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
        mockSuccessfulResponse("/current");

        // WHEN
        final List<Consolidation> result = client.getCurrentBacklog(
                WAREHOUSE_ID,
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
        mockSuccessfulResponse("/current");

        // WHEN
        final List<Consolidation> result = client.getCurrentBacklog(
                new BacklogCurrentRequest(WAREHOUSE_ID)
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
    void testGetBacklogErr() {
        // GIVEN
        mockErroneousResponse();
        BacklogRequest request = new BacklogRequest(
                DATE_CURRENT,
                WAREHOUSE_ID,
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
        assertThrows(ClientException.class, () ->
                client.getBacklog(request)
        );
    }

    private void mockSuccessfulResponse(String query) throws JSONException {
        final String url = String.format(URL, WAREHOUSE_ID) + query;

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL + url)
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(buildResponse().toString())
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

    private void mockErroneousResponse() {
        final String url = String.format(URL, WAREHOUSE_ID) + "?" + QUERY_PARAMS;

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL + url)
                .withStatusCode(INTERNAL_SERVER_ERROR.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();
    }
}
