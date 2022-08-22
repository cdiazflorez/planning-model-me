package com.mercadolibre.planning.model.me.clients.rest.outboundsettings;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_SETTING;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.SettingsGateway;
import com.mercadolibre.planning.model.me.gateways.outboundsettings.dtos.SettingsAtWarehouse;
import com.mercadolibre.restclient.MeliRestClient;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OutboundSettingsClient extends HttpClient implements SettingsGateway {

  private static final String SETTINGS_URL = "/wms/warehouses/%s/outbound/settings/picking";

  public OutboundSettingsClient(final MeliRestClient client) {
    super(client, OUTBOUND_SETTING.name());
  }

  @Override
  public SettingsAtWarehouse getPickingSetting(String logisticCenterId) {
    final HttpRequest request = HttpRequest.builder()
        .url(format(SETTINGS_URL, logisticCenterId))
        .GET()
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(request, response -> response.getData(new TypeReference<>() {
    }));
  }
}
