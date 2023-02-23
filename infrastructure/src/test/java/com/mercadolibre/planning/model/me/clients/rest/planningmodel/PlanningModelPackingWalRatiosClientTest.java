package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanningModelPackingWalRatiosClientTest extends BaseClientTest {

  private static final String GET_PACKING_RATIOS_URL = "/logistic_center/%s/ratios";

  private PlanningModelPackingWalRatiosClient client;


  @BeforeEach
  void setUp() throws IOException {
    client = new PlanningModelPackingWalRatiosClient(getRestTestClient());
  }


  @Test
  void testGetPackingWallRatios() {

    //GIVEN
    final String url = String.format(GET_PACKING_RATIOS_URL, "LC123") + "/packing_wall";

    // Arrange
    final String logisticCenterId = "LC123";
    final Instant dateFrom = Instant.parse("2022-01-15T10:00:00Z");
    final Instant dateTo = Instant.parse("2022-02-15T10:00:00Z");

    final Map<Instant, PackingRatio> expectedResult = new ConcurrentHashMap<>();

    MockResponse.builder()
        .withMethod(GET)
        .withURL(BASE_URL_MELISYSTEMS + url)
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(expectedResult.toString())
        .build();

    final var response = client.getPackingWallRatios(logisticCenterId, dateFrom, dateTo);
    assertNotNull(response);
    assertEquals(expectedResult, response);

  }
}
