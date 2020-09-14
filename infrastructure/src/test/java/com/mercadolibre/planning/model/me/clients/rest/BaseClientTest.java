package com.mercadolibre.planning.model.me.clients.rest;

import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.mock.RequestMockHolder;

import java.io.IOException;

public class BaseClientTest {

    protected static final String BASE_URL = "http://internal.mercadolibre.com";

    protected RestClient getRestTestClient() throws IOException {

        final RestClientConfig.PlanningModelClientProperties planningModelClientProperties =
                new RestClientConfig.PlanningModelClientProperties();
        planningModelClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.OutboundUnitRestClientProperties outboundUnitRestClientProperties =
                new RestClientConfig.OutboundUnitRestClientProperties();
        outboundUnitRestClientProperties.setBaseUrl(BASE_URL);

        return new RestClientConfig(
                planningModelClientProperties, outboundUnitRestClientProperties
        ).restClient();
    }

    public void cleanMocks() {
        RequestMockHolder.clear();
    }
}
