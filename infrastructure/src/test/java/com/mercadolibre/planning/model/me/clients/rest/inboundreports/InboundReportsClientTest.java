package com.mercadolibre.planning.model.me.clients.rest.inboundreports;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.inboundreports.dto.InboundResponse;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InboundReportsClientTest extends BaseClientTest {

  private static final String URL = "/wms/inbound-reports?";
  private static final String DATE_FROM = "2023-01-13T05:00:00Z";
  private static final String DATE_TO = "2023-01-13T15:38:00Z";
  private static final String SHIPMENT = "transfer";
  private static final String WH = "COCU02";

  private InboundReportsClient client;

  @BeforeEach
  public void setUp() throws Exception {
    client = new InboundReportsClient(getRestTestClient());
  }

  @Test
  public void testUnitsReceived() throws IOException {
    //GIVEN
    MockResponse.builder()
        .withMethod(GET)
        .withStatusCode(SC_OK)
        .withURL(BaseClientTest.BASE_URL
            + URL
            + format("last_arrival_date_from=%s&last_arrival_date_to=%s&shipment_type=%s&warehouse_id=%s",
            DATE_FROM, DATE_TO, SHIPMENT, WH))
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString("get_units_received.json"))
        .build();

    //WHEN
    final InboundResponse response = client.getUnitsReceived(WH, Instant.parse(DATE_FROM), Instant.parse(DATE_TO), SHIPMENT);

    //THEN
    Assertions.assertNotNull(response);
    Assertions.assertEquals(1, response.getAggregations().size());
    Assertions.assertEquals(197026, response.getAggregations().get(0).getValue());


  }

}
