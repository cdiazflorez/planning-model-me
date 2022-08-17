package com.mercadolibre.planning.model.me.clients.rest.staffing;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.STAFFING;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.request.MetricRequest;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.MetricResponse;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StaffingApiClient extends HttpClient implements StaffingGateway {

  private static final String STAFFING_URL = "/logistic_centers/%s/metrics";

  public StaffingApiClient(final MeliRestClient client) {
    super(client, STAFFING.name());
  }

  @Trace
  @Override
  public StaffingResponse getStaffing(final String logisticCenter) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(STAFFING_URL, logisticCenter))
        .GET()
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  @Trace
  @Override
  public MetricResponse getMetricsByName(String logisticCenter, String metricName, MetricRequest metricRequest) {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put("processes", metricRequest.getProcessName().getName());
    params.put("date_from", metricRequest.getDateFrom().toString());
    params.put("date_to", metricRequest.getDateTo().toString());
    final HttpRequest request = HttpRequest.builder()
        .url(format(STAFFING_URL + "/%s", logisticCenter, metricName))
        .GET()
        .queryParams(params)
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

}
