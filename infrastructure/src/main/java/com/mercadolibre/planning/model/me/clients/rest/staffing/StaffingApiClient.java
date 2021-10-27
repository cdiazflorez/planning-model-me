package com.mercadolibre.planning.model.me.clients.rest.staffing;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.gateways.staffing.StaffingGateway;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingResponse;
import com.mercadolibre.restclient.MeliRestClient;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.STAFFING;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

@Component
public class StaffingApiClient extends HttpClient implements StaffingGateway {

    private static final String STAFFING_URL = "/logistic_centers/%s/metrics";

    public StaffingApiClient(final MeliRestClient client) {
        super(client, STAFFING.name());
    }

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

}
