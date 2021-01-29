package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.UnitGroup;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationFilterRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotal;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitRequest;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitSorter;
import com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.response.OutboundUnitSearchResponse;
import com.mercadolibre.planning.model.me.config.JsonUtilsConfiguration;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
import com.mercadolibre.planning.model.me.usecases.monitor.dtos.GetMonitorInput;
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

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.OutboundUnitClient.CLIENT_ID;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitAggregationRequestTotalOperation.SUM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.ETD_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.ETD_TO;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitFilterRequestStringValue.STATUS;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitOrdering.ASC;
import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request.SearchUnitProperty.ESTIMATED_TIME_DEPARTURE;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.OUTBOUND_PLANNING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessInfo.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
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
    class SearchUnitGroups {
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
            assertEquals("OUTBOUND_UNIT", exception.getApiName());
            assertEquals("/wms/outbound/groups/order/search", exception.getPath());
            assertEquals(POST, exception.getHttpMethod());
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
                    .filter(new SearchUnitAggregationFilterRequest(List.of(
                            Map.of(SearchUnitFilterRequestStringValue.WAREHOUSE_ID.toJson(),
                                    "BRSP01"),
                            Map.of(SearchUnitFilterRequestStringValue.GROUP_TYPE.toJson(), "order"),
                            Map.of(STATUS.toJson(), "pending")))
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
                    .filter(new SearchUnitAggregationFilterRequest(List.of(
                            Map.of(SearchUnitFilterRequestStringValue.WAREHOUSE_ID.toJson(),
                                    "BRSP01"),
                            Map.of(SearchUnitFilterRequestStringValue.GROUP_TYPE.toJson(), "order"),
                            Map.of(STATUS.toJson(), "pending")))
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
        @DisplayName("Units API returns OK")
        public void testGetBacklog() throws JsonProcessingException, JSONException {
            // GIVEN
            final ZonedDateTime currentTime = ZonedDateTime.now().withMinute(0).withSecond(0);
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
                                                    .put(currentTime.plusHours(1).toString())
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
                                                    .put(currentTime.plusHours(2).toString())
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
                                                    .put(currentTime.plusHours(3).toString())
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 200)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put(currentTime.minusDays(1).toString())
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 37)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put("undefined")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 37)
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
            assertEquals(currentTime.plusHours(1), backlogCpt1.getDate());
            assertEquals(114, backlogCpt1.getQuantity());

            final Backlog backlogCpt2 = backlogs.get(1);
            assertEquals(currentTime.plusHours(2), backlogCpt2.getDate());
            assertEquals(754, backlogCpt2.getQuantity());

            final Backlog backlogCpt3 = backlogs.get(2);
            assertEquals(currentTime.plusHours(3), backlogCpt3.getDate());
            assertEquals(200, backlogCpt3.getQuantity());
        }

        @Test
        @DisplayName("Units API returns OK by Process Backlog")
        public void testGetProcessBacklog() throws JsonProcessingException, JSONException {
            // GIVEN
            final ZonedDateTime utcDateFrom = getCurrentUtcDate();
            final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

            final Map<String, Object> requestBody = ImmutableMap.<String, Object>builder()
                    .put("limit", 0)
                    .put("offset", 0)
                    .put("filter", singletonMap("and", asList(
                            singletonMap("warehouse_id", "ARTW01"),
                            singletonMap("group_type", GROUP_TYPE),
                            singletonMap(ETD_FROM, utcDateFrom),
                            singletonMap(ETD_TO, utcDateTo),
                            singletonMap("or", List.of(
                                    Map.of(STATUS.toJson(), "pending"),
                                    Map.of(STATUS.toJson(), "to_pick"),
                                    Map.of(STATUS.toJson(), "to_pack"),
                                    Map.of(STATUS.toJson(), "to_group")
                                    )
                            ))))
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
                                                    .put("to_pack")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 754)
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


            GetMonitorInput input = GetMonitorInput.builder()
                    .warehouseId(TestUtils.WAREHOUSE_ID)
                    .dateTo(utcDateTo)
                    .dateFrom(utcDateFrom)
                    .build();

            // WHEN
            final String status = "status";
            List<Map<String, String>> statuses = List.of(
                    Map.of(status, OUTBOUND_PLANNING.getStatus()),
                    Map.of(status, PACKING.getStatus())
            );
            final List<ProcessBacklog> backlogs = outboundUnitClient.getBacklog(statuses,
                    input.getWarehouseId(),
                    input.getDateFrom(),
                    input.getDateTo());

            // THEN
            assertEquals(1, backlogs.size());

            final ProcessBacklog backlogCpt2 = backlogs.get(0);
            assertEquals("to_pack", backlogCpt2.getProcess());
            assertEquals(754, backlogCpt2.getQuantity());
        }

        @Test
        @DisplayName("Units API returns OK by Unit Process Backlog")
        public void testGetUnitProcessBacklog() throws JsonProcessingException, JSONException {
            // GIVEN
            final ZonedDateTime utcDateFrom = getCurrentUtcDate();
            final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

            final Map<String, String> requestParam = ImmutableMap.<String, String>builder()
                    .put("limit", "1")
                    .put("group.etd_from", utcDateFrom.toString())
                    .put("group.etd_to", utcDateTo.toString())
                    .put("status", PICKING.getStatus())
                    .put("client.id", CLIENT_ID)
                    .put("warehouse_id", TestUtils.WAREHOUSE_ID)
                    .build();

            final JSONObject responseBody = new JSONObject()
                    .put("paging", new JSONObject().put("total", "100"))
                    .put("results", new JSONArray())
                    ;

            successfulResponse(
                    GET,
                    searchUnitUrl(requestParam),
                    null,
                    responseBody.toString()
            );

            // WHEN
            final ProcessBacklog backlogs = outboundUnitClient.getUnitBacklog(
                    new UnitProcessBacklogInput(PICKING.getStatus(),
                    TestUtils.WAREHOUSE_ID,
                    utcDateFrom,
                    utcDateTo, null));

            // THEN
            assertEquals(PICKING.getStatus(), backlogs.getProcess());
            assertEquals(100, backlogs.getQuantity());
        }

        @Test
        @DisplayName("Units API returns OK by Unit Process Backlog with area")
        public void testGetUnitProcessBacklogWithArea()
                throws JsonProcessingException, JSONException {
            // GIVEN
            final ZonedDateTime utcDateFrom = getCurrentUtcDate();
            final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

            final Map<String, String> requestParam = ImmutableMap.<String, String>builder()
                    .put("limit", "1")
                    .put("group.etd_from", utcDateFrom.toString())
                    .put("group.etd_to", utcDateTo.toString())
                    .put("status", PACKING.getStatus())
                    .put("client.id", CLIENT_ID)
                    .put("warehouse_id", TestUtils.WAREHOUSE_ID)
                    .put("address.area", "PW")
                    .build();

            final JSONObject responseBody = new JSONObject()
                    .put("paging", new JSONObject().put("total", "100"))
                    .put("results", new JSONArray());

            successfulResponse(
                    GET,
                    searchUnitUrl(requestParam),
                    null,
                    responseBody.toString()
            );

            // WHEN
            final ProcessBacklog backlogs = outboundUnitClient.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING.getStatus(),
                            TestUtils.WAREHOUSE_ID,
                            utcDateFrom,
                            utcDateTo, "PW")
            );

            // THEN
            assertEquals(PACKING.getStatus(), backlogs.getProcess());
            assertEquals(100, backlogs.getQuantity());
        }

        @Test
        @DisplayName("OU returns quantities grouped by CPT OK")
        public void testGetSales() throws JsonProcessingException, JSONException {
            // GIVEN
            final ZonedDateTime currentTime =
                    ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
            final String dateCreatedFrom = currentTime.minusHours(28).format(ISO_OFFSET_DATE_TIME);

            final Map<String, Object> requestBody = ImmutableMap.<String, Object>builder()
                    .put("limit", 0)
                    .put("offset", 0)
                    .put("filter", singletonMap("and", asList(
                            singletonMap("warehouse_id", "ARTW01"),
                            singletonMap("group_type", GROUP_TYPE),
                            singletonMap("date_created_from", dateCreatedFrom)
                    )))
                    .put("aggregations", singletonList(
                            ImmutableMap.<String, Object>builder()
                                    .put("name", "by_etd")
                                    .put("keys", singletonList("etd"))
                                    .put("totals", singletonList(
                                            ImmutableMap.<String, String>builder()
                                                    .put("operation", "sum")
                                                    .put("operand", "$quantity")
                                                    .put("alias", "ventas")
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
                                                    .put(currentTime.plusHours(1).toString())
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "ventas")
                                                            .put("result", 800)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put(currentTime.plusHours(2).toString())
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "ventas")
                                                            .put("result", 700)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put(currentTime.plusHours(3).toString())
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "ventas")
                                                            .put("result", 500)
                                                    )
                                            )
                                    )
                                    .put(new JSONObject()
                                            .put("keys", new JSONArray()
                                                    .put("undefined")
                                            )
                                            .put("totals", new JSONArray()
                                                    .put(new JSONObject()
                                                            .put("alias", "total_units")
                                                            .put("result", 37)
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
            final List<Backlog> backlogs =
                    outboundUnitClient.getSalesByCpt(TestUtils.WAREHOUSE_ID, dateCreatedFrom);

            // THEN
            assertEquals(3, backlogs.size());

            final Backlog backlogCpt1 = backlogs.get(0);
            assertEquals(currentTime.plusHours(1), backlogCpt1.getDate());
            assertEquals(800, backlogCpt1.getQuantity());

            final Backlog backlogCpt2 = backlogs.get(1);
            assertEquals(currentTime.plusHours(2), backlogCpt2.getDate());
            assertEquals(700, backlogCpt2.getQuantity());

            final Backlog backlogCpt3 = backlogs.get(2);
            assertEquals(currentTime.plusHours(3), backlogCpt3.getDate());
            assertEquals(500, backlogCpt3.getQuantity());
        }

        private String searchGroupUrl(final String groupType) {
            return UnitGroupUrlBuilder
                    .create(format("/groups/%s/search", groupType))
                    .withParams(ImmutableMap.of("client.id", "9999"))
                    .build();
        }

        private String searchUnitUrl(final Map<String, String> params) {
            return UnitGroupUrlBuilder
                    .create("/units/search")
                    .withParams(params)
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

    private JSONObject dummyUnitGroupJson(final String value) throws JSONException {
        return new JSONObject()
                .put("id", value);
    }

    private static class UnitGroupUrlBuilder {
        private static final String DOMAIN = "http://internal.mercadolibre.com";
        private static final String BASE_PATH = "/wms/outbound";

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
