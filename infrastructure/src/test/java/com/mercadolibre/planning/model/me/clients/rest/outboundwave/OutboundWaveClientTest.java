package com.mercadolibre.planning.model.me.clients.rest.outboundwave;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.restclient.MockResponse;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
public class OutboundWaveClientTest extends BaseClientTest {

    private static final String OUTBOUND_WAVE_URL = "/wms/outbound/wave_considered_units/count";
    private OutboundWaveClient outboundWaveClient;

    @BeforeEach
    public void setUp() throws Exception {
        RequestMockHolder.clear();
        outboundWaveClient = new OutboundWaveClient(getRestTestClient());
    }

    @Nested
    @DisplayName("Test get units count")
    class UnitsCountWaves {

        @Test
        @DisplayName("get units count correctly")
        void testUnitsCountCorrect() {

            // GIVEN
            final ZoneId zoneId = ZoneId.of("America/Bogota");
            final ZonedDateTime dateFrom = ZonedDateTime.of(2019, 12, 20, 19, 4,0, 0, zoneId);
            final ZonedDateTime dateTo = ZonedDateTime.of(2019, 12, 20, 20, 4, 0, 0, zoneId);
            final String unitGroupType = "ORDER";

            givenMockGetCountUnitsWaves("9");

            //WHEN
            final long result =
                    whenGetUnitsCount(
                            WAREHOUSE_ID, dateFrom, dateTo, unitGroupType
                    );

            //THEN
            assertEquals(9L, result);
        }

        @Test
        @DisplayName("with warehouse not exist")
        void testUnitsCountWithWarehouseNotExist() {

            // GIVEN
            final String warehouseId = "AAA000";
            final ZoneId zoneId = ZoneId.of("America/Bogota");
            final ZonedDateTime dateFrom = ZonedDateTime.of(2019, 12, 20, 19, 4, 0, 0, zoneId);
            final ZonedDateTime dateTo = ZonedDateTime.of(2019, 12, 20, 20, 4, 0, 0, zoneId);
            final String unitGroupType = "ORDER";

            givenMockGetCountUnitsWaves("0");

            //WHEN
            final long result =
                    whenGetUnitsCount(
                            warehouseId, dateFrom, dateTo, unitGroupType
                    );

            //THEN
            assertNotNull(result);
            assertEquals(0L, result);
        }

        @Test
        @DisplayName("with parameters empty or null")
        void testUnitsCountWithParametersEmptyOrNull() {

            // GIVEN
            final String warehouseId = "";
            final ZonedDateTime dateFrom = ZonedDateTime.now();
            final ZonedDateTime dateTo = ZonedDateTime.now().plusHours(1);
            final String unitGroupType = null;

            givenMockGetCountUnitsWavesBadRequest();

            //WHEN
            assertThrows(ClientException.class, () -> whenGetUnitsCount(
                    warehouseId, dateFrom, dateTo, unitGroupType
            ));
        }

        private long whenGetUnitsCount(final String warehouseId,
                                       final ZonedDateTime dateFrom,
                                       final ZonedDateTime dateTo,
                                       final String unitGroupType) {
            return outboundWaveClient.getUnitsCount(
                    warehouseId, dateFrom, dateTo, unitGroupType);
        }

        private void givenMockGetCountUnitsWaves(String response) {
            MockResponse.builder()
                    .withMethod(GET)
                    .withURL(BASE_URL + OUTBOUND_WAVE_URL)
                    .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                    .withStatusCode(HttpStatus.OK.value())
                    .withResponseBody(response)
                    .build();
        }

        private void givenMockGetCountUnitsWavesBadRequest() {
            StringBuilder response = new StringBuilder()
                    .append("\"status\": \"BAD_REQUEST\",\n")
                    .append("\"message\":org.springframework.validation.BeanPropertyBindingResult:")
                    .append("2 errors\\nField error in object 'unitsCountRequest'")
                    .append(" \"error\": \"invalid_body\"");

            MockResponse.builder()
                    .withMethod(GET)
                    .withURL(BASE_URL + OUTBOUND_WAVE_URL)
                    .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                    .withStatusCode(HttpStatus.BAD_REQUEST.value())
                    .withResponseBody(response.toString())
                    .build();
        }
    }
}
