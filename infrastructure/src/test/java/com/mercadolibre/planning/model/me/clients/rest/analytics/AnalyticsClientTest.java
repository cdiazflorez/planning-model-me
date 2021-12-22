package com.mercadolibre.planning.model.me.clients.rest.analytics;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.entities.projection.AnalyticsQueryEvent;
import com.mercadolibre.planning.model.me.entities.projection.UnitsResume;
import com.mercadolibre.restclient.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PACKING_WALL;
import static com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata.process.ProcessOutbound.PICKING;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalyticsClientTest extends BaseClientTest {

    private static final String ANALYTICS_URL = "/wms/warehouses/%s/metric/count";

    private AnalyticsClient client;

    @BeforeEach
    void setUp() throws IOException {
        client = new AnalyticsClient(getRestTestClient());
    }

    @AfterEach
    void tearDown() {
        super.cleanMocks();
    }

    @Test
    void testGetUnitsInInterval() throws IOException {

        // GIVEN
        final int hoursOffset = 1;
        final List<AnalyticsQueryEvent> eventType = List.of(
                AnalyticsQueryEvent.PACKING_NO_WALL,
                AnalyticsQueryEvent.PICKING
                );

        MockResponse.builder()
                .withMethod(GET)
                .withURL(BASE_URL + format(ANALYTICS_URL, WAREHOUSE_ID))
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(
                        getResourceAsString("analytics_units_query_response.json"))
                .build();


        //WHEN
        final List<UnitsResume> unitsResume =
                client.getUnitsInInterval(WAREHOUSE_ID, hoursOffset, eventType);

        //THEN
        assertNotNull(unitsResume);
        assertEquals(3, unitsResume.size());
        assertNotNull(unitsResume.stream().filter(unit -> Objects.equals(unit.getProcess(),
                AnalyticsQueryEvent.PACKING_WALL)).findFirst().orElse(null));
        assertNotNull(unitsResume.stream().filter(unit -> Objects.equals(unit.getProcess(),
                AnalyticsQueryEvent.PICKING)).findFirst().orElse(null));

        assertEquals(PICKING.getTitle(), AnalyticsQueryEvent.PICKING.getRelatedProcess());
        assertEquals(PACKING.getTitle(), AnalyticsQueryEvent.PACKING_NO_WALL.getRelatedProcess());
        assertEquals(PACKING_WALL.getTitle(),
                AnalyticsQueryEvent.PACKING_WALL.getRelatedProcess());
    }


}
