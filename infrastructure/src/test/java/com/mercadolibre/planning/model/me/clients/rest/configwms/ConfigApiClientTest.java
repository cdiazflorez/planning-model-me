package com.mercadolibre.planning.model.me.clients.rest.configwms;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.GET;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.me.clients.rest.BaseClientTest;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigApiClientTest extends BaseClientTest {

  private static final String URL = "/process-paths/outbound/%s";

  private static final String LOGISTIC_CENTER = "ARTW01";

  private static final String RESPONSE_BODY_RESOURCE = "get_process_paths_config.json";

  private static final String STATUS_ACTIVE = "ACTIVE";

  private static final String STATUS_INACTIVE = "INACTIVE";

  private static final List<MockResponseConfigApi> MOCK_RESPONSE = List.of(
      new MockResponseConfigApi("PP_TRANSFER", STATUS_ACTIVE),
      new MockResponseConfigApi("PP_DEFAULT_MULTI", STATUS_INACTIVE),
      new MockResponseConfigApi("NON_TOT_MONO", STATUS_ACTIVE)
  );

  private ConfigApiClient client;

  @BeforeEach
  public void setUp() throws Exception {
    client = new ConfigApiClient(getRestTestClient());
    cleanMocks();
  }

  @Test
  public void testGetProcessPath() throws IOException {
    //GIVEN
    MockResponse.builder()
        .withMethod(GET)
        .withStatusCode(SC_OK)
        .withURL(BaseClientTest.BASE_URL + format(URL, LOGISTIC_CENTER))
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .withResponseBody(getResourceAsString(RESPONSE_BODY_RESOURCE))
        .build();

    final var response = client.getProcessPath(LOGISTIC_CENTER);

    final var processPathResponse = response.getProcessPaths();

    assertNotNull(processPathResponse);
    assertEquals(processPathResponse.size(), MOCK_RESPONSE.size());

    for (var i = 0; i < processPathResponse.size(); i++) {
      var processPathExpected = MOCK_RESPONSE.get(i);
      var processPath = processPathResponse.get(i);

      assertEquals(processPathExpected.getProcessPathCode(), processPath.getProcessPathCode());
      assertEquals(processPathExpected.getStatus(), processPath.getStatus());
    }
  }

  @Test
  public void testGetProcessPathError() {
    //GIVEN
    MockResponse.builder()
        .withMethod(GET)
        .withStatusCode(SC_INTERNAL_SERVER_ERROR)
        .withURL(BaseClientTest.BASE_URL + format(URL, LOGISTIC_CENTER))
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .build();

    assertThrows(ClientException.class, () -> client.getProcessPath(LOGISTIC_CENTER));
  }

  @Value
  @AllArgsConstructor
  private static class MockResponseConfigApi {
    String processPathCode;
    String status;
  }
}
