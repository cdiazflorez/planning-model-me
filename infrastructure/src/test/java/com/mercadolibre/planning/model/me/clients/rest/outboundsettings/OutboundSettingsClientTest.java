package com.mercadolibre.planning.model.me.clients.rest.outboundsettings;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos.SettingsAtWarehouse;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
public class OutboundSettingsClientTest extends BaseClientTest {

  private static final String WAREHOUSE_ID = "ARTW01";

  private OutboundSettingsClient client;

  @BeforeEach
  public void setUp() throws Exception {
    client = new OutboundSettingsClient(getRestTestClient());
  }

  @Test
  public void pickingSettingTest() throws IOException {

    // GIVEN
    MockResponse.builder()
        .withMethod(GET)
        .withStatusCode(SC_OK)
        .withURL(format(BaseClientTest.BASE_URL
            + "/wms/warehouses/%s/outbound/settings/picking", WAREHOUSE_ID))
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString("get_picking_settings_response.json"))
        .build();

    // WHEN
    final SettingsAtWarehouse response = client.getPickingSetting(WAREHOUSE_ID);

    // THEN
    assertNotNull(response);
    assertEquals(2, response.getAreas().size());
    assertEquals("DM", response.getAreas().get(0).getId());

  }

}
