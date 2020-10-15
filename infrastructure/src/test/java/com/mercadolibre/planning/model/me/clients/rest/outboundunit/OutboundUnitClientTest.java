package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.UnitGroup;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotal;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitSorter;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.planning.model.me.config.JsonUtilsConfiguration;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import com.mercadolibre.restclient.MockResponse;
import com.mercadolibre.restclient.http.HttpMethod;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotalOperation.SUM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARDINALITY;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.CARRIER_NAME;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.STATUS;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitOrdering.ASC;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitProperty.ESTIMATED_TIME_DEPARTURE;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class) // we need it to use jsontest
@JsonTest
public class OutboundUnitClientTest extends BaseClientTest {

    private OutboundUnitClient outboundUnitClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        outboundUnitClient = new OutboundUnitClient(getRestTestClient(), objectMapper);
        new JsonUtilsConfiguration().run(null);
        RequestMockHolder.clear();
    }

    @Nested
    @DisplayName("Test Search")
    class SearchUnit {
        private static final String GROUP_TYPE = "order";

        @Test
        @DisplayName("Rest Client failed")
        public void throwsException() {
            // GIVEN
            final SearchUnitRequest searchUnitRequest =
                    new SearchUnitRequest(0, 0, null, emptyList(), emptyList());
            failingResponse(POST, searchGroupUrl(GROUP_TYPE));

            // WHEN
            final ClientException exception = assertThrows(ClientException.class,
                    () -> outboundUnitClient.searchGroups(GROUP_TYPE, searchUnitRequest)
            );

            // THEN
            assertEquals(exception.getApiName(), "OUTBOUND_UNIT");
            assertEquals(exception.getPath(), "/wms/outbound/groups/order/search");
            assertEquals(exception.getHttpMethod(), POST);
            assertNotNull(exception.getCause());
        }

        @Test
        @DisplayName("Units API returns 500")
        public void unknownError() {
            // GIVEN
            final SearchUnitRequest searchUnitRequest =
                    new SearchUnitRequest(0, 0, null, emptyList(), emptyList());
            unsuccessfulResponse(POST, searchGroupUrl(GROUP_TYPE), INTERNAL_SERVER_ERROR);

            // WHEN
            final ClientException exception = assertThrows(ClientException.class,
                    () -> outboundUnitClient.searchGroups(GROUP_TYPE, searchUnitRequest)
            );

            // THEN
            assertEquals(500, exception.getResponseStatus().intValue());
            assertTrue(exception.getOtherParams().containsKey("client.id"));
            assertNull(exception.getCause());
        }

        @Test
        public void notFound() {
            // GIVEN
            final SearchUnitRequest searchUnitRequest =
                    new SearchUnitRequest(0, 0, null, emptyList(), emptyList());
            unsuccessfulResponse(POST, searchGroupUrl(GROUP_TYPE), NOT_FOUND);

            // WHEN
            final Executable executable =
                    () -> outboundUnitClient.searchGroups(GROUP_TYPE, searchUnitRequest);

            // THEN
            assertThrows(ClientException.class, executable);
        }

        @Test
        @DisplayName("Units API returns a collection of units")
        public void sunnyCase() throws JSONException, JsonProcessingException {
            // GIVEN
            final SearchUnitRequest searchUnitRequest = SearchUnitRequest.builder()
                    .limit(10)
                    .offset(0)
                    .filter(
                            SearchUnitFilterRequest.and(
                                    SearchUnitFilterRequest.string(
                                            SearchUnitFilterRequestStringValue.WAREHOUSE_ID,
                                            "BRSP01"
                                    ),
                                    SearchUnitFilterRequest.string(
                                            SearchUnitFilterRequestStringValue.GROUP_TYPE,
                                            GROUP_TYPE
                                    ),
                                    SearchUnitFilterRequest.string(STATUS, "pending"),
                                    SearchUnitFilterRequest.or(
                                            SearchUnitFilterRequest.and(
                                                    SearchUnitFilterRequest.string(
                                                            CARDINALITY,
                                                            "multi"
                                                    ),
                                                    SearchUnitFilterRequest.string(
                                                            CARRIER_NAME,
                                                            "correios"
                                                    )
                                            ),
                                            SearchUnitFilterRequest.and(
                                                    SearchUnitFilterRequest
                                                            .string(CARDINALITY, "multi"),
                                                    SearchUnitFilterRequest
                                                            .string(CARRIER_NAME, "fedex")
                                            )
                                    )
                            )
                    )
                    .sorters(List.of(
                            new SearchUnitSorter(ESTIMATED_TIME_DEPARTURE, ASC)
                    ))
                    .build();

            final Map<String, Object> body = ImmutableMap.<String, Object>builder()
                    .put("limit", 10)
                    .put("offset", 0)
                    .put("filter", singletonMap("and", asList(
                            singletonMap("warehouse_id", "BRSP01"),
                            singletonMap("group_type", GROUP_TYPE),
                            singletonMap("status", "pending"),
                            singletonMap("or", asList(
                                    singletonMap("and", asList(
                                            singletonMap("cardinality", "multi"),
                                            singletonMap("carrier_name", "correios")
                                    )),
                                    singletonMap("and", asList(
                                            singletonMap("cardinality", "multi"),
                                            singletonMap("carrier_name", "fedex")
                                    ))
                            ))
                    )))
                    .put("sorters", singletonList(ImmutableMap.<String, Object>builder()
                            .put("property", "ESTIMATED_TIME_DEPARTURE")
                            .put("ordering","ASC")
                            .build()))
                    .build();

            successfulResponse(
                    POST,
                    searchGroupUrl(GROUP_TYPE),
                    objectMapper.writeValueAsString(body),
                    searchBody(
                            new JSONArray()
                                    .put(dummyUnitGroupJson(valueOf(1L)))
                                    .put(dummyUnitGroupJson(valueOf(2L)))
                    )
            );

            // WHEN
            final OutboundUnitSearchResponse<UnitGroup> response =
                    outboundUnitClient.searchGroups(GROUP_TYPE, searchUnitRequest);

            // THEN
            assertEquals(
                    asList(1L, 2L),
                    response.getResults().stream().map(UnitGroup::getId).collect(toList())
            );
        }

        @Test
        @DisplayName("Units API returns an aggregation response")
        public void sunnyAggregationCase() throws JSONException, JsonProcessingException {
            // GIVEN
            final SearchUnitRequest searchUnitRequest = SearchUnitRequest.builder()
                    .limit(10)
                    .offset(0)
                    .filter(
                            SearchUnitFilterRequest.and(
                                    SearchUnitFilterRequest.string(
                                            SearchUnitFilterRequestStringValue.WAREHOUSE_ID,
                                            "BRSP01"
                                    ),
                                    SearchUnitFilterRequest.string(
                                            SearchUnitFilterRequestStringValue.GROUP_TYPE,
                                            GROUP_TYPE
                                    ),
                                    SearchUnitFilterRequest.string(STATUS, "pending"),
                                    SearchUnitFilterRequest.or(
                                            SearchUnitFilterRequest.and(
                                                    SearchUnitFilterRequest.string(
                                                            CARDINALITY,
                                                            "multi"
                                                    ),
                                                    SearchUnitFilterRequest.string(
                                                            CARRIER_NAME,
                                                            "correios"
                                                    )
                                            ),
                                            SearchUnitFilterRequest.and(
                                                    SearchUnitFilterRequest
                                                            .string(CARDINALITY, "multi"),
                                                    SearchUnitFilterRequest
                                                            .string(CARRIER_NAME, "fedex")
                                            )
                                    )
                            )
                    )
                    .aggregations(singletonList(
                            new SearchUnitAggregationRequest(
                                    "total_units",
                                    singletonList("etd"),
                                    singletonList(
                                            SearchUnitAggregationRequestTotal.builder()
                                                    .alias("total_units")
                                                    .operand("$quantity")
                                                    .operation(SUM)
                                                    .build()
                                    )
                            )
                    ))
                    .build();

            final Map<String, Object> body = ImmutableMap.<String, Object>builder()
                    .put("limit", 10)
                    .put("offset", 0)
                    .put("filter", singletonMap("and", asList(
                            singletonMap("warehouse_id", "BRSP01"),
                            singletonMap("group_type", GROUP_TYPE),
                            singletonMap("status", "pending"),
                            singletonMap("or", asList(
                                    singletonMap("and", asList(
                                            singletonMap("cardinality", "multi"),
                                            singletonMap("carrier_name", "correios")
                                    )),
                                    singletonMap("and", asList(
                                            singletonMap("cardinality", "multi"),
                                            singletonMap("carrier_name", "fedex")
                                    ))
                            ))
                    )))
                    .put("aggregations", singletonList(
                            ImmutableMap.<String, Object>builder()
                                    .put("name", "total_units")
                                    .put("totals", singletonList(
                                            ImmutableMap.<String, String>builder()
                                                    .put("operation", "sum")
                                                    .put("operand", "$quantity")
                                                    .put("alias", "total_units")
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            successfulResponse(
                    POST,
                    searchGroupUrl(GROUP_TYPE),
                    objectMapper.writeValueAsString(body),
                    searchBodyWithAggregations(
                            new JSONObject()
                                    .put("alias", "total_units")
                                    .put("result", 3)
                    ));

            // WHEN
            final OutboundUnitSearchResponse<UnitGroup> response =
                    outboundUnitClient.searchGroups(GROUP_TYPE, searchUnitRequest);

            // THEN
            assertEquals(
                    List.of(),
                    response.getResults()
            );
            assertEquals(
                    "total_units",
                    response.getAggregations().get(0).getName()
            );
            assertEquals(
                    List.of(),
                    response.getAggregations().get(0)
                            .getBuckets().get(0)
                            .toSummaryBucket("total_units").getKeys()
            );
            assertEquals(
                    3L,
                    response.getAggregations().get(0)
                            .getBuckets().get(0)
                            .toSummaryBucket("total_units").getTotal()
            );
        }

        @Test
        @DisplayName("OU API returns OK")
        public void testGetBacklog() throws JsonProcessingException, JSONException {
            // GIVEN
            final Map<String, Object> requestBody = ImmutableMap.<String, Object>builder()
                    .put("limit", 0)
                    .put("offset", 0)
                    .put("filter", singletonMap("and", asList(
                            singletonMap("warehouse_id", "ARTW01"),
                            singletonMap("group_type", GROUP_TYPE),
                            singletonMap("status", "pending")
                    )))
                    .put("aggregations", singletonList(
                            ImmutableMap.<String, Object>builder()
                                    .put("name", "by_etd")
                                    .put("keys", singletonList("etd"))
                                    .put("totals", singletonList(
                                            ImmutableMap.<String, String>builder()
                                                    .put("operation", "sum")
                                                    .put("operand", "$quantity")
                                                    .put("alias", "total_units")
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            final JSONObject responseBody = new JSONObject()
                    .put("paging", new JSONObject().put("totals", "10"))
                    .put("results", new JSONArray())
                    .put("aggregations", new JSONArray().put(new JSONObject()
                            .put("name", "by_etd")
                            .put("buckets", new JSONArray()
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put("2020-10-07T13:00Z[UTC]")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 114)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put("2020-10-07T09:00Z[UTC]")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 754)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put("2020-10-07T06:00Z[UTC]")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 200)
                                                    )
                                            )
                                    )
                            )
                    ));

            successfulResponse(
                    POST,
                    searchGroupUrl(GROUP_TYPE),
                    objectMapper.writeValueAsString(requestBody),
                    responseBody.toString()
            );

            // WHEN
            final List<Backlog> backlogs = outboundUnitClient.getBacklog(TestUtils.WAREHOUSE_ID);

            // THEN
            assertEquals(3, backlogs.size());

            final Backlog backlogCpt1 = backlogs.get(0);
            assertEquals(ZonedDateTime.parse("2020-10-07T13:00Z[UTC]"), backlogCpt1.getDate());
            assertEquals(114, backlogCpt1.getQuantity());

            final Backlog backlogCpt2 = backlogs.get(1);
            assertEquals(ZonedDateTime.parse("2020-10-07T09:00Z[UTC]"), backlogCpt2.getDate());
            assertEquals(754, backlogCpt2.getQuantity());

            final Backlog backlogCpt3 = backlogs.get(2);
            assertEquals(ZonedDateTime.parse("2020-10-07T06:00Z[UTC]"), backlogCpt3.getDate());
            assertEquals(200, backlogCpt3.getQuantity());
        }

        private String searchGroupUrl(final String groupType) {
            return UnitGroupUrlBuilder
                    .create(format("/%s/search", groupType))
                    .withParams(ImmutableMap.of("client.id", "9999"))
                    .build();
        }

        private String searchBody(final JSONArray results) throws JSONException {
            return new JSONObject()
                    .put("paging", new JSONObject().put("totals", "10"))
                    .put("results", results)
                    .toString();
        }

        private String searchBodyWithAggregations(final JSONObject aggregationBucket)
                throws JSONException {

            return new JSONObject()
                    .put("paging", new JSONObject().put("totals", "10"))
                    .put("results", new JSONArray())
                    .put("aggregations", new JSONArray().put(new JSONObject()
                            .put("name", "total_units")
                            .put("buckets", new JSONArray()
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray())
                                            .put("totals", new JSONArray()
                                                    .put(aggregationBucket))))))
                    .toString();
        }
    }

    private void failingResponse(final HttpMethod httpMethod, final String url) {
        MockResponse.builder()
                .withMethod(httpMethod)
                .withURL(url)
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .shouldFail()
                .build();
    }

    private void unsuccessfulResponse(final HttpMethod httpMethod,
                                      final String url,
                                      final HttpStatus httpStatus) {
        MockResponse.builder()
                .withMethod(httpMethod)
                .withURL(url)
                .withStatusCode(httpStatus.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();
    }

    private void successfulResponse(final HttpMethod httpMethod,
                                    final String url,
                                    final String requestBody,
                                    final String responseBody) {

        MockResponse.Builder builder = MockResponse.builder()
                .withMethod(httpMethod)
                .withURL(url)
                .withStatusCode(OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(responseBody);

        if (requestBody != null) {
            builder = builder.withRequestBody(requestBody);
        }

        builder.build();
    }

    private JSONObject dummyUnitJson(final String id, final String status) throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("status", status)
                .put("group",
                        new JSONObject()
                                .put(
                                        "estimated_departure_time",
                                        "2019-03-08T12:00:00Z")
                );
    }

    private JSONObject dummyUnitGroupJson(final String value) throws JSONException {
        return new JSONObject()
                .put("id", value);
    }

    private static class UnitUrlBuilder {
        private static final String DOMAIN = "http://internal.mercadolibre.com";
        private static final String BASE_PATH = "/wms/outbound/units";

        private final String path;
        private final Map<String, String> params;

        public UnitUrlBuilder(final String path, final Map<String, String> params) {
            this.path = path;
            this.params = params;
        }

        public static UnitUrlBuilder create(final String path) {
            return new UnitUrlBuilder(path, new HashMap<>());
        }

        public UnitUrlBuilder withParams(final Map<String, String> params) {
            this.params.putAll(params);
            return this;
        }

        public String build() {
            return format("%s%s%s?%s",
                    DOMAIN,
                    BASE_PATH,
                    path,
                    params.entrySet().stream()
                            .map(entry -> format("%s=%s", entry.getKey(), entry.getValue()))
                            .collect(joining("&")));
        }
    }

    private static class UnitGroupUrlBuilder {
        private static final String DOMAIN = "http://internal.mercadolibre.com";
        private static final String BASE_PATH = "/wms/outbound/groups";

        private final String path;
        private final Map<String, String> params;

        public UnitGroupUrlBuilder(final String path, final Map<String, String> params) {
            this.path = path;
            this.params = params;
        }

        public static UnitGroupUrlBuilder create(final String path) {
            return new UnitGroupUrlBuilder(path, new HashMap<>());
        }

        public UnitGroupUrlBuilder withParams(final Map<String, String> params) {
            this.params.putAll(params);
            return this;
        }

        public String build() {
            return format("%s%s%s?%s",
                    DOMAIN,
                    BASE_PATH,
                    path,
                    params.entrySet().stream()
                            .map(entry -> format("%s=%s", entry.getKey(), entry.getValue()))
                            .collect(joining("&")));
        }
    }
}
