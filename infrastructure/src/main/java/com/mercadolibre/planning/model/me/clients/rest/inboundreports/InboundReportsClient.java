package com.mercadolibre.planning.model.me.clients.rest.inboundreports;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.INBOUND_REPORTS;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.inboundreports.InboundReportsApiGateway;
import com.mercadolibre.planning.model.me.gateways.inboundreports.dto.InboundResponse;
import com.mercadolibre.restclient.MeliRestClient;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InboundReportsClient extends HttpClient implements InboundReportsApiGateway {

  private static final String URL = "/wms/inbound-reports?";

  public InboundReportsClient(MeliRestClient client) {
    super(client, INBOUND_REPORTS.name());
  }

  @Override
  public InboundResponse getUnitsReceived(String warehouseId, Instant lastArrivalDateFrom, Instant lastArrivalDateTo, String shipmentType) {
    final HttpRequest request = HttpRequest.builder()
        .url(URL)
        .GET()
        .queryParams(createQueryParams(warehouseId, lastArrivalDateFrom, lastArrivalDateTo, shipmentType))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }

  private Map<String, String> createQueryParams(
      final String warehouseId,
      final Instant lastArrivalDateFrom,
      final Instant lastArrivalDateTo,
      final String shipmentType) {

    Map<String, String> params = new ConcurrentHashMap<>();
    params.put("last_arrival_date_from", lastArrivalDateFrom.toString());
    params.put("last_arrival_date_to", lastArrivalDateTo.toString());
    params.put("warehouse_id", warehouseId);

    if (shipmentType != null) {
      params.put("shipment_type", shipmentType);
    }
    return params;
  }
}
