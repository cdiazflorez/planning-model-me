package com.mercadolibre.planning.model.me.clients.rest.backlog;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.clients.rest.backlog.request.BacklogRequest;
import com.mercadolibre.planning.model.me.clients.rest.backlog.response.Backlog;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

class BacklogApiClientTest extends BaseClientTest {
    private static final String URL = "/flow/backlogs/logistic_centers/%s/backlogs";

    private static final String QUERY_PARAMS =
            "workflows=outbound_orders&date_from=2021-01-01T00:00&date_to=2021-01-02T05:00";

    private static final String WORKFLOW = "outbound_order";

    private static final ZonedDateTime DATE_FROM =
            parse("2021-08-12T01:00:00Z", ISO_OFFSET_DATE_TIME);

    private static final ZonedDateTime DATE_TO =
            parse("2021-08-12T05:00:00Z", ISO_OFFSET_DATE_TIME);

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
        final List<Backlog> result = client.getBacklog(request);

        // THEN
        assertEquals(3, result.size());

        final Backlog firstBacklog = result.get(0);
        assertEquals(1255, firstBacklog.getTotal());
        assertEquals(DATE_FROM, firstBacklog.getDate());

        final Map<String, String> keys = firstBacklog.getKeys();
        assertEquals("wms-outbound", keys.get("workflow"));
        assertEquals("picking", keys.get("process"));
        assertEquals("MZ-0", keys.get("area"));
        assertEquals("2021-01-02T00:00", keys.get("date_out"));
    }

    @Test
    void testGetBacklogErr() {
        // GIVEN
        mockErroneousResponse();
        BacklogRequest request = BacklogRequest.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflows(of(WORKFLOW))
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();

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
