package com.mercadolibre.planning.model.me.clients.rest.staffing;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Area;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingProcess;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingWorkflowResponse;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class StaffingApiClientTest extends BaseClientTest {

    private static final String WAREHOUSE_ID = "ARBA01";

    private StaffingApiClient client;

    @BeforeEach
    public void setUp() throws Exception {
        client = new StaffingApiClient(getRestTestClient());
    }

    @Test
    void testGetStaffing() throws IOException {

        // GIVEN
        whenMockTestGetStaffing();

        // WHEN
        final StaffingResponse staffingResponse = client.getStaffing(WAREHOUSE_ID);

        // THEN
        assertNotNull(staffingResponse);
        assertNotNull(staffingResponse.getWorkflows());
        assertEquals(2, staffingResponse.getWorkflows().size());

        final StaffingWorkflowResponse inbound = staffingResponse.getWorkflows().get(0);
        assertEquals("fbm-wms-inbound", inbound.getName());
        assertEquals(3, inbound.getTotals().getIdle());
        assertEquals(10, inbound.getTotals().getWorkingSystemic());
        assertEquals(5, inbound.getTotals().getWorkingNonSystemic());
        assertEquals(2, inbound.getProcesses().size());

        final StaffingProcess putAway = inbound.getProcesses().get(1);
        assertEquals("put_away", putAway.getName());
        assertEquals(1, putAway.getTotals().getIdle());
        assertEquals(8, putAway.getTotals().getWorkingSystemic());
        assertEquals(102.30, putAway.getTotals().getNetProductivity());
        assertEquals(150.00, putAway.getTotals().getThroughput());
        assertEquals(2, putAway.getAreas().size());

        final Area rkH = putAway.getAreas().get(0);
        assertEquals("RK-H", rkH.getName());
        assertEquals(0, rkH.getTotals().getIdle());
        assertEquals(2, rkH.getTotals().getWorkingSystemic());
        assertEquals(183.03, rkH.getTotals().getProductivity());
        assertEquals(23.0, rkH.getTotals().getThroughput());
    }

    private void whenMockTestGetStaffing() throws IOException {

        MockResponse.builder()
                .withMethod(GET)
                .withStatusCode(SC_OK)
                .withURL(format(BaseClientTest.BASE_URL
                        + "/logistic_centers/%s/metrics", WAREHOUSE_ID))
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .withResponseBody(getResourceAsString("staffing_search_metrics_response.json"))
                .build();
    }
}
