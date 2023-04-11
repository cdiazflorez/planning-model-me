package com.mercadolibre.planning.model.me.clients.rest.configwms;

import static com.mercadolibre.planning.model.me.enums.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.me.enums.ProcessPath.TOT_MULTI_BATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Context by understand tests.
 * A PP is valid, if it is added in processPath ENUMS else it is invalid.
 * Adapter only return PP valid and actives with type ProcessPath
 */
@ExtendWith(MockitoExtension.class)
public class ProcessPathAdapterTest {

  private static final String LOGISTIC_CENTER = "ARTW01";

  private static final String STATUS_ACTIVE = "ACTIVE";

  private static final String STATUS_INACTIVE = "INACTIVE";

  private static final int EMPTY_LIST = 0;

  private static final List<ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig> PROCESS_PATH_CODES = List.of(
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("PP_TRANSFER", STATUS_ACTIVE),
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("PP_DEFAULT_MULTI", STATUS_INACTIVE),
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("NON_TOT_MONO", STATUS_ACTIVE),
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("TOT_MULTI_BATCH", STATUS_ACTIVE)
  );

  private static final List<ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig> PROCESS_PATH_CODE_INVALID = List.of(
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("PP_TRANSFER", STATUS_ACTIVE)
  );

  private static final List<ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig> PROCESS_PATH_CODE_INACTIVE = List.of(
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("NON_TOT_MONO", STATUS_INACTIVE),
      new ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig("TOT_MULTI_BATCH", STATUS_INACTIVE)
  );

  private static final Set<ProcessPath> PROCESS_PATHS = Set.of(NON_TOT_MONO, TOT_MULTI_BATCH);

  @InjectMocks
  private ProcessPathAdapter processPathAdapter;

  @Mock
  private ConfigApiClient configApiClient;

  private static Stream<Arguments> entityResponse() {
    return Stream.of(
        Arguments.of(new ConfigApiClient.ProcessPathConfigWrapper(PROCESS_PATH_CODE_INVALID)),
        Arguments.of(new ConfigApiClient.ProcessPathConfigWrapper(PROCESS_PATH_CODE_INACTIVE))
    );
  }

  @Test
  @DisplayName("Response PP valid and actives")
  void testGetProcessPath() {
    //GIVEN
    when(configApiClient.getProcessPath(LOGISTIC_CENTER))
        .thenReturn(new ConfigApiClient.ProcessPathConfigWrapper(PROCESS_PATH_CODES));

    //WHEN
    var response = processPathAdapter.getProcessPathGateway(LOGISTIC_CENTER);

    //THEN
    assertEquals(PROCESS_PATHS.size(), response.size());

    for (var processPathResponse : response) {
      assertTrue(PROCESS_PATHS.contains(processPathResponse));
    }
  }

  @ParameterizedTest
  @MethodSource("entityResponse")
  @DisplayName("As all PP are invalids, adapter return an empty list")
  void testGetOnlyProcessPathInvalids(final ConfigApiClient.ProcessPathConfigWrapper mockResponse) {
    //GIVEN
    when(configApiClient.getProcessPath(LOGISTIC_CENTER))
        .thenReturn(mockResponse);

    //WHEN
    var response = processPathAdapter.getProcessPathGateway(LOGISTIC_CENTER);

    //THEN
    assertNotNull(response);
    assertEquals(EMPTY_LIST, response.size());
  }

  @Test
  @ParameterizedTest
  @MethodSource("entityResponse")
  @DisplayName("As all PP are inactive, adapter return an empty list")
  void testGetOnlyProcessPathInactive() {
    //GIVEN
    when(configApiClient.getProcessPath(LOGISTIC_CENTER))
        .thenReturn(new ConfigApiClient.ProcessPathConfigWrapper(PROCESS_PATH_CODE_INACTIVE));

    //WHEN
    var response = processPathAdapter.getProcessPathGateway(LOGISTIC_CENTER);

    //THEN
    assertNotNull(response);
    assertEquals(EMPTY_LIST, response.size());
  }
}
