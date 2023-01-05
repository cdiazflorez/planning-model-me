package com.mercadolibre.planning.model.me.clients.rest.outboundunit;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.config.JsonUtilsConfiguration;
import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.restclient.MockResponse;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class) // we need it to use jsontest
@JsonTest
public class OutboundUnitSearchClientTest extends BaseClientTest {

  private static final String UNITS_SEARCH_URL =
      "/wms/warehouses/%s/outbound/units/search?client.id=%d";
  private OutboundUnitSearchClient outboundUnitSearchClient;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() throws Exception {
    outboundUnitSearchClient = new OutboundUnitSearchClient(getRestTestClient(), objectMapper);
    new JsonUtilsConfiguration().run(null);
    RequestMockHolder.clear();
  }

  @Test
  public void testExecuteGetSalesByCpt() throws IOException {
    mockCptService();
    final List<Backlog> sales = outboundUnitSearchClient
        .getSalesByCpt(createCptFilterRequest());

    assertFalse(sales.isEmpty());
    assertEquals(13, sales.size());
    assertTrue(sales.stream().allMatch(sale -> sale.getQuantity() == 300));
  }

  private BacklogFilters createCptFilterRequest() {
    final ZonedDateTime dateTo = ZonedDateTime.of(2021, 2, 3, 16, 0, 0, 0, ZoneId.of("UTC"));
    return BacklogFilters.builder()
        .warehouseId(WAREHOUSE_ID_ARTW01)
        .cptFrom(dateTo.minusHours(28))
        .cptTo(dateTo)
        .timeZone(ZoneId.of("UTC"))
        .build();
  }

  private void mockCptService() throws IOException {

    MockResponse.builder()
        .withMethod(POST)
        .withURL(BASE_URL + format(UNITS_SEARCH_URL, WAREHOUSE_ID_ARTW01, 9999))
        .withStatusCode(OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withRequestBody(getResourceAsString("unit_search_reports_request.json"))
        .withResponseBody(
            getResourceAsString("unit_search_reports_response.json"))
        .build();
  }
}
