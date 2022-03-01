package com.mercadolibre.planning.model.me.utils;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.planning.model.me.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.EntityRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.RunSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SaveSimulationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.SimulationRequest;
import com.mercadolibre.resilience.breaker.CircuitBreaker;
import com.mercadolibre.resilience.breaker.CircuitBreakers;
import com.mercadolibre.restclient.MockResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

/**
 * Build methods and constants to be used in testing.
 */
public class TestUtils {
  public static final String WAREHOUSE_ID = "ARTW01";

  public static final Long USER_ID = 1234L;

  public static final ZonedDateTime A_DATE = ZonedDateTime.of(2020, 8, 19, 17, 40, 0, 0,
      ZoneId.of("UTC"));

  public static final String CALLER_ID = "caller.id";

  public static final String OUTBOUND_WORKFLOW = "fbm-wms-outbound";

  public static final String INBOUND_WORKFLOW = "fbm-wms-inbound";

  public static final String WITHDRAWALS_WORKFLOW = "fbm-wms-withdrawals";

  public static final String RECEIVING_PROCESS = "receiving";

  public static final String CHECK_IN_PROCESS = "check_in";

  public static final String PICKING_PROCESS = "picking";

  public static final String PACKING_PROCESS = "packing";

  public static final String AREA_MZ1 = "MZ-1";

  public static final long C_BREAKER_INTERVAL = 100;

  public static final long C_BREAKER_TRY_WINDOW = 100;

  public static final double C_BREAKER_COEFFICIENT = 0.5;

  public static final int C_BREAKER_BUCKETS = 10;

  public static final long C_BREAKER_MS_BUCKET = 100;

  public static final double C_BREAKER_MIN_SCORE = 0.5;

  public static final int C_BREAKER_STALE_INTERVAL = 100;

  public static final int C_BREAKER_MIN_MEASURES = 100;

  public static final String C_BREAKER_RESOURCE = "UnitsBreaker";

  public static final String HEADCOUNT = "headcount";

  public static final double DEVIATION_VALUE = 10.0;

  /**
   * Builds a known object mapper, ready to use in test.
   * @return an {@link ObjectMapper} instance
   */
  public static ObjectMapper objectMapper() {
    return ((JsonJackson) JsonUtils.INSTANCE.getEngine())
        .getMapper()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
  }

  /**
   * Mocks a Success Response for the given params.
   * @param url     url to build the Response
   * @param request to be assumed as ok
   */
  public static void mockPostUrlSuccess(final String url, final JSONObject request) {
    MockResponse.builder()
        .withMethod(POST)
        .withURL(url)
        .withRequestBody(request.toString())
        .withStatusCode(HttpStatus.OK.value())
        .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
        .build();
  }

  /**
   * Uses a class loader to get the resource and returns it as String.
   * @param resourceName to process
   * @return the toString value of the resource
   * @throws IOException when class loader fails
   * @throws IllegalStateException from IOException
   */
  public static String getResourceAsString(final String resourceName) throws IOException {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final InputStream resource = classLoader.getResourceAsStream(resourceName);

    try {
      return IOUtils.toString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      resource.close();
    }
  }

  /**
   * Mocks an CircuitBreaker -> ExponentialBreaker from known values
   * <p>resource: {@value C_BREAKER_RESOURCE}</p>
   * <p>interval: {@value C_BREAKER_INTERVAL}</p>
   * <p>tryWindow: {@value C_BREAKER_TRY_WINDOW}</p>
   * <p>coefficient: {@value C_BREAKER_COEFFICIENT}</p>
   * <p>buckets: {@value C_BREAKER_BUCKETS}</p>
   * <p>bucketWithMs: {@value C_BREAKER_MS_BUCKET}</p>
   * <p>minScore: {@value C_BREAKER_MIN_SCORE}</p>
   * <p>staleInterval: {@value C_BREAKER_STALE_INTERVAL}</p>
   * <p>minMeasures: {@value C_BREAKER_MIN_MEASURES}</p>.
   *
   * @return a new {@link CircuitBreaker} instance
   */
  public static CircuitBreaker mockCircuitBreaker() {
    return CircuitBreakers.newExponentialBreaker(
        C_BREAKER_RESOURCE,
        C_BREAKER_INTERVAL,
        C_BREAKER_TRY_WINDOW,
        C_BREAKER_COEFFICIENT,
        C_BREAKER_BUCKETS,
        C_BREAKER_MS_BUCKET,
        C_BREAKER_MIN_SCORE,
        C_BREAKER_STALE_INTERVAL,
        C_BREAKER_MIN_MEASURES
    );
  }

  /**
   * Mocks a Run Simulation Request from known values:
   * <p>warehouseId: {@value WAREHOUSE_ID}</p>
   * <p>SimulationRequest.processName: {@value PICKING_PROCESS}</p>
   * <p>SimulationRequest.entities(EntityRequest.type): {@value HEADCOUNT}.</p>
   *
   * @return a {@link RunSimulationRequest} instance
   */
  public static RunSimulationRequest mockRunSimulationRequest() {
    return RunSimulationRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .simulations(List.of(SimulationRequest.builder()
            .processName(PICKING_PROCESS)
            .entities(List.of(EntityRequest.builder()
                .type(HEADCOUNT)
                .build()))
            .build()))
        .build();
  }

  /**
   * Mocks a SaveSimulationRequest from known values:
   * <p>warehouseId: {@value WAREHOUSE_ID}</p>
   * <p>simulations(SimulationRequest.processName): {@value PICKING_PROCESS}</p>
   * <p>SimulationRequest.entities(EntityRequest.type): {@value HEADCOUNT}.</p>
   *
   * @return a {@link SaveSimulationRequest} instance
   */
  public static SaveSimulationRequest mockSaveSimulationRequest() {
    return SaveSimulationRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .simulations(List.of(SimulationRequest.builder()
            .processName(PICKING_PROCESS)
            .entities(List.of(EntityRequest.builder()
                .type(HEADCOUNT)
                .build()))
            .build()))
        .build();
  }

  /**
   * Mocks a DeviationRequest from known values:
   * <p>warehouseId: {@value WAREHOUSE_ID}</p>
   * <p>value: {@value DEVIATION_VALUE}.</p>
   * @param hours top value from now in date time range
   * @return a {@link DeviationRequest} instance
   */
  public static DeviationRequest mockDeviationRequest(final long hours) {

    final ZonedDateTime dateFrom = ZonedDateTime.now();
    final ZonedDateTime dateTo = dateFrom.plusHours(hours);

    return DeviationRequest.builder()
        .warehouseId(WAREHOUSE_ID)
        .value(DEVIATION_VALUE)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .build();
  }
}
