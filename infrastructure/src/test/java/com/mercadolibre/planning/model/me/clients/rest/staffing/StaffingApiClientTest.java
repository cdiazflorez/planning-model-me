package com.mercadolibre.planning.model.me.clients.rest.staffing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.clients.rest.staffing.request.StaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.Aggregation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.GetStaffingRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.Operation;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.restclient.MockResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.utils.TestUtils.objectMapper;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Map.entry;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
public class StaffingApiClientTest extends BaseClientTest {

    private StaffingApiClient client;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {

        objectMapper = objectMapper();

        client = new StaffingApiClient(getRestTestClient(), objectMapper);
    }

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final ZonedDateTime dateFrom = parse("2022-03-04T00:00:00Z");
    private static final ZonedDateTime dateTo = parse("2022-03-05T00:00:00Z");
    private static final String WAREHOUSE_ID = "ARBA01";

    @Test
    public void testGetStaffing() throws JSONException, JsonProcessingException {

        // GIVEN
        final GetStaffingRequest getStaffingRequest =
                new GetStaffingRequest(
                        dateFrom,
                        dateTo,
                        WAREHOUSE_ID,
                        getAggregations());

        whenMockTestGetStaffing(getBodyResponse());

        // WHEN
        final StaffingResponse response = client.getStaffing(getStaffingRequest);

        // THEN
        final com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Aggregation
                aggregation = response.getAggregations().get(0);

        assertEquals(aggregation.getName(), "agg1");

        assertEquals(aggregation.getResults().get(0).getKeys(),
                List.of("fbm_wms_outbound", "picking", "working"));
        assertEquals(aggregation.getResults().get(0)
                .getOperations().get(0).getAlias(), "net_productivity");
        assertEquals(aggregation.getResults().get(0)
                .getOperations().get(0).getResult(), 56.01);
        assertEquals(aggregation.getResults().get(0)
                .getOperations().get(1).getAlias(), "workers_count");
        assertEquals(aggregation.getResults().get(0)
                .getOperations().get(1).getResult(), 120);

        assertEquals(aggregation.getResults().get(1).getKeys(),
                List.of("fbm_wms_outbound", "packing", "working"));
        assertEquals(aggregation.getResults().get(1)
                .getOperations().get(0).getAlias(), "net_productivity");
        assertEquals(aggregation.getResults().get(1)
                .getOperations().get(0).getResult(), 59.01);
        assertEquals(aggregation.getResults().get(1)
                .getOperations().get(1).getAlias(), "workers_count");
        assertEquals(aggregation.getResults().get(1)
                .getOperations().get(1).getResult(), 20);
    }

    private void whenMockTestGetStaffing(final JSONObject bodyResponse)
            throws JsonProcessingException {

        Map<String, String> filters = Map.ofEntries(
                entry("synchronizationDateFrom", dateFrom.format(DATE_FORMATTER)),
                entry("synchronizationDateTo", dateTo.format(DATE_FORMATTER)),
                entry("logisticCenterId", WAREHOUSE_ID));

        final StaffingRequest staffingRequest = new StaffingRequest(filters, getAggregations());

        MockResponse.builder()
                .withMethod(POST)
                .withStatusCode(SC_OK)
                .withURL(format(BaseClientTest.BASE_URL
                        + "/fbm/flow/staffing/logistic_centers/%s/metrics", WAREHOUSE_ID))
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withRequestBody(objectMapper.writeValueAsString(staffingRequest))
                .withResponseBody(bodyResponse.toString())
                .build();
    }

    private List<Aggregation> getAggregations() {

        return List.of(
                new Aggregation(
                        "agg1",
                        List.of("process",
                                "subprocess",
                                "worker_status"),
                        List.of(
                                new Operation(
                                        "net_productivity",
                                        "id",
                                        "count"),
                                new Operation(
                                        "net_productivity",
                                        "net_productivity",
                                        "avg"))));
    }

    private JSONObject getBodyResponse() throws JSONException {

        return new JSONObject()
                .put("aggregations", new JSONArray().put(
                        new JSONObject()
                                .put("name", "agg1")
                                .put("results", new JSONArray()
                                        .put(new JSONObject()
                                                .put("keys", new JSONArray()
                                                        .put("fbm_wms_outbound")
                                                        .put("picking")
                                                        .put("working"))
                                                .put("operations", new JSONArray()
                                                        .put(new JSONObject()
                                                                .put("alias","net_productivity")
                                                                .put("result","56.01"))
                                                        .put(new JSONObject()
                                                                .put("alias","workers_count")
                                                                .put("result","120")))
                                        ).put(new JSONObject()
                                                .put("keys", new JSONArray()
                                                        .put("fbm_wms_outbound")
                                                        .put("packing")
                                                        .put("working"))
                                                .put("operations", new JSONArray()
                                                        .put(new JSONObject()
                                                                .put("alias","net_productivity")
                                                                .put("result","59.01"))
                                                        .put(new JSONObject()
                                                                .put("alias","workers_count")
                                                                .put("result","20")))))));
    }
}
