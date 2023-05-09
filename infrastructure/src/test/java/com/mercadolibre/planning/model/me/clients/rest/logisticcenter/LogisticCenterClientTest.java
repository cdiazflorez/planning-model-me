package com.mercadolibre.planning.model.me.clients.rest.logisticcenter;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static java.util.TimeZone.getTimeZone;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.restclient.MockResponse;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LogisticCenterClientTest extends BaseClientTest {

    private LogisticCenterClient client;

    private static final String WAREHOUSE_ID = "ARTW01";

    @BeforeEach
    public void setUp() throws IOException {
        RequestMockHolder.clear();
        client = new LogisticCenterClient(getRestTestClient());
    }

    @Test
    @DisplayName("get logisticCenter configuration should return a configuration")
    public void testGetLogisticCenterConfigurationOk() throws JSONException {
        // GIVEN
        final JSONObject body = new JSONObject().put("time_zone", "CST");

        mockGetLogisticCenterConfiguration(WAREHOUSE_ID, body);

        final LogisticCenterConfiguration logisticCenterConfiguration =
                new LogisticCenterConfiguration(getTimeZone("CST"));

        // WHEN
        final LogisticCenterConfiguration response =
                client.getConfiguration(WAREHOUSE_ID);

        // THEN
        assertEquals(logisticCenterConfiguration, response);
    }

    @Test
    @DisplayName("get logisticCenter configuration should return a configuration with put to wall")
    public void testGetLogisticCenterConfigurationOkWhenHavePutToWall() throws JSONException {
        // GIVEN
        final JSONObject body = new JSONObject().put("time_zone", "CST")
                .put("outbound", new JSONObject().put("put_to_wall", "true"));

        mockGetLogisticCenterConfiguration(WAREHOUSE_ID, body);

        final LogisticCenterConfiguration logisticCenterConfiguration =
                new LogisticCenterConfiguration(getTimeZone("CST"), true);

        // WHEN
        final LogisticCenterConfiguration response =
                client.getConfiguration(WAREHOUSE_ID);

        // THEN
        assertEquals(logisticCenterConfiguration, response);
    }

    @Test
    @DisplayName("get logisticCenter configuration empty should return a configuration")
    public void testGetLogisticCenterConfigurationEmpty() {
        // GIVEN
        final JSONObject body = new JSONObject();
        mockGetLogisticCenterConfiguration(WAREHOUSE_ID, body);

        final LogisticCenterConfiguration logisticCenterConfiguration =
                new LogisticCenterConfiguration(getTimeZone("UTC"));

        // WHEN
        final LogisticCenterConfiguration response =
                client.getConfiguration(WAREHOUSE_ID);

        // THEN
        assertEquals(logisticCenterConfiguration, response);
    }

    private void mockGetLogisticCenterConfiguration(final String warehouseId,
                                                    final JSONObject body) {
        MockResponse.builder()
                .withMethod(GET)
                .withStatusCode(SC_OK)
                .withURL(format(BASE_URL + "/logistic_centers/%s/configurations", warehouseId))
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(body.toString())
                .build();
    }
}
