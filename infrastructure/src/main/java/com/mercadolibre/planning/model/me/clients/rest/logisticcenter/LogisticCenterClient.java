package com.mercadolibre.planning.model.me.clients.rest.logisticcenter;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.clients.rest.logisticcenter.response.LogisticCenterResponse;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.restclient.MeliRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TimeZone;

import static java.util.Objects.nonNull;

@Component
public class LogisticCenterClient extends HttpClient implements LogisticCenterGateway {

    private static final String URL = "/logistic_centers/%s/configurations";

    public LogisticCenterClient(final MeliRestClient restClient) {
        super(restClient, RestPool.LOGISTIC_CENTER.name());
    }

    public LogisticCenterConfiguration getConfiguration(final String warehouseId) {
        final HttpRequest request = HttpRequest.builder()
                .url(String.format(URL, warehouseId))
                .GET()
                .acceptedHttpStatuses(Set.of(HttpStatus.OK))
                .build();

        return toLcConfiguration(send(request, response ->
                response.getData(new TypeReference<>() {})));
    }

    private LogisticCenterConfiguration toLcConfiguration(final LogisticCenterResponse response) {
        boolean hasPutToWall = nonNull(response.getOutbound())
                && response.getOutbound().isPutToWall();
        return new LogisticCenterConfiguration(response.getTimeZone() == null
                ? TimeZone.getTimeZone("UTC") : TimeZone.getTimeZone(response.getTimeZone()),
                hasPutToWall);
    }
}
