package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.config.JsonUtilsConfiguration;
import com.mercadolibre.planning.model.me.entities.projection.ProcessBacklog;
import com.mercadolibre.planning.model.me.gateways.backlog.UnitProcessBacklogInput;
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
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.mercadolibre.planning.model.me.clients.rest.outboundunit.OutboundUnitClient.CLIENT_ID;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.mockCircuitBreaker;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class) // we need it to use jsontest
@JsonTest
public class OutboundUnitClientTest extends BaseClientTest {

    private OutboundUnitClient outboundUnitClient;

    @BeforeEach
    public void setUp() throws Exception {
        outboundUnitClient = new OutboundUnitClient(getRestTestClient(), mockCircuitBreaker());
        new JsonUtilsConfiguration().run(null);
        RequestMockHolder.clear();
    }

    @Nested
    @DisplayName("Test Search")
    class SearchUnitGroups {
        private static final String GROUP_TYPE = "order";

        @Test
        @DisplayName("Units API returns OK by Unit Process Backlog")
        public void testGetUnitProcessBacklog() throws JSONException {
            // GIVEN
            final ZonedDateTime utcDateFrom = getCurrentUtcDate();
            final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

            final Map<String, String> requestParam = ImmutableMap.<String, String>builder()
                    .put("limit", "1")
                    .put("group.etd_from", utcDateFrom.toString())
                    .put("group.etd_to", utcDateTo.toString())
                    .put("status", PICKING.getStatus())
                    .put("client.id", CLIENT_ID)
                    .put("warehouse_id", WAREHOUSE_ID_ARTW01)
                    .build();

            final JSONObject responseBody = new JSONObject()
                    .put("paging", new JSONObject().put("total", "100"))
                    .put("results", new JSONArray())
                    ;

            successfulResponse(
                    GET,
                    searchUnitUrl(requestParam, WAREHOUSE_ID_ARTW01),
                    null,
                    responseBody.toString()
            );

            // WHEN
            final ProcessBacklog backlogs = outboundUnitClient.getUnitBacklog(
                    new UnitProcessBacklogInput(PICKING.getStatus(),
                        WAREHOUSE_ID_ARTW01,
                    utcDateFrom,
                    utcDateTo, null, "order"));

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
                    .put("warehouse_id", WAREHOUSE_ID_ARTW01)
                    .put("address.area", "PW")
                    .build();

            final JSONObject responseBody = new JSONObject()
                    .put("paging", new JSONObject().put("total", "100"))
                    .put("results", new JSONArray());

            successfulResponse(
                    GET,
                    searchUnitUrl(requestParam, WAREHOUSE_ID_ARTW01),
                    null,
                    responseBody.toString()
            );

            // WHEN
            final ProcessBacklog backlogs = outboundUnitClient.getUnitBacklog(
                    new UnitProcessBacklogInput(PACKING.getStatus(),
                        WAREHOUSE_ID_ARTW01,
                            utcDateFrom,
                            utcDateTo, "PW", GROUP_TYPE));

            // THEN
            assertEquals(PACKING.getStatus(), backlogs.getProcess());
            assertEquals(100, backlogs.getQuantity());
        }

        @Test
        @DisplayName("Search Units API returns 500")
        public void unknownErrorUnits() {
            // GIVEN
            final ZonedDateTime utcDateFrom = getCurrentUtcDate();
            final ZonedDateTime utcDateTo = utcDateFrom.plusDays(1);

            final Map<String, String> requestParam = ImmutableMap.<String, String>builder()
                    .put("limit", "1")
                    .put("group.etd_from", utcDateFrom.toString())
                    .put("group.etd_to", utcDateTo.toString())
                    .put("status", PACKING.getStatus())
                    .put("client.id", CLIENT_ID)
                    .put("warehouse_id", WAREHOUSE_ID_ARTW01)
                    .put("address.area", "PW")
                    .build();
            unsuccessfulResponse(GET, searchUnitUrl(requestParam, WAREHOUSE_ID_ARTW01),
                    INTERNAL_SERVER_ERROR);

            // WHEN
            final ClientException exception = assertThrows(ClientException.class,
                    () -> outboundUnitClient.getUnitBacklog(
                    new UnitProcessBacklogInput(PICKING.getStatus(),
                        WAREHOUSE_ID_ARTW01,
                            utcDateFrom,
                            utcDateTo, null, GROUP_TYPE))
            );

            // THEN
            assertEquals(0, exception.getResponseStatus().intValue());
            assertTrue(exception.getOtherParams().containsKey("client.id"));
            assertNotNull(exception.getCause());
            assertTrue(exception.getCause() instanceof ClientException);
            assertEquals(500, ((ClientException) exception.getCause())
                    .getResponseStatus().intValue());
            assertNull(exception.getCause().getCause());
        }

        private String searchUnitUrl(final Map<String, String> params, final String warehouseId) {
            return UnitGroupUrlBuilder
                    .create(String.format("/wms/warehouses/%s/outbound/units/search", warehouseId))
                    .withParams(params)
                    .build();
        }

        private String searchBody(final JSONArray results) throws JSONException {
            return new JSONObject()
                    .put("paging", new JSONObject().put("totals", "10"))
                    .put("results", results)
                    .toString();
        }
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

    private static class UnitGroupUrlBuilder {
        private static final String DOMAIN = "http://internal.mercadolibre.com";

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
            return format("%s%s?%s",
                    DOMAIN,
                    path,
                    params.entrySet().stream()
                            .map(entry -> format("%s=%s", entry.getKey(), entry.getValue()))
                            .collect(joining("&")));
        }
    }
}
