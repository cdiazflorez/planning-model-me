package com.mercadolibre.planning.model.me.clients.rest.planningmodel;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL_RATIOS;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.PackingWallRatiosGateway;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanningModelPackingWalRatiosClient extends HttpClient implements PackingWallRatiosGateway {

  private static final String PACKING_RATIOS_URL = "/logistic_center/%s/ratios";

  protected PlanningModelPackingWalRatiosClient(MeliRestClient client) {
    super(client, PLANNING_MODEL_RATIOS.name());
  }

  @Trace
  @Override
  public Map<Instant, PackingRatio> getPackingWallRatios(String logisticCenterId, Instant dateFrom, Instant dateTo) {
    final Map<String, String> params = new ConcurrentHashMap<>();
    params.put("date_from", dateFrom.toString());
    params.put("date_to", dateTo.toString());

    final HttpRequest request = HttpRequest.builder()
        .url(format(PACKING_RATIOS_URL + "/packing_wall", logisticCenterId))
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }
}
