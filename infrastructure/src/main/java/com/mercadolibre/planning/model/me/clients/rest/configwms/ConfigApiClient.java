package com.mercadolibre.planning.model.me.clients.rest.configwms;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.CONFIG;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Set;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigApiClient extends HttpClient {
  private static final String PROCESS_PATH_URL = "/process-paths/outbound/%s";

  protected ConfigApiClient(MeliRestClient client) {
    super(client, CONFIG.name());
  }

  /**
   * Get process path by logistic Center.
   *
   * @param logisticCenterId logisticCenterId
   * @return ProcessPathConfig is represents object of a necessary client response part.
   */
  @Trace
  public ProcessPathConfigWrapper getProcessPath(final String logisticCenterId) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(PROCESS_PATH_URL, logisticCenterId))
        .GET()
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Value
  public static class ProcessPathConfigWrapper {
    List<ProcessPathConfig> processPaths;

    @Value
    public static class ProcessPathConfig {
      String processPathCode;
      String status;
    }
  }

}
