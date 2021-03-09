package com.mercadolibre.planning.model.me.clients.rest;

import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.AnalyticsClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.AuthorizationClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.LogisticCenterClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.PlanningModelClientProperties;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.mock.RequestMockHolder;

import java.io.IOException;

public class BaseClientTest {

    protected static final String BASE_URL = "http://internal.mercadolibre.com";

    protected MeliRestClient getRestTestClient() throws IOException {

        final PlanningModelClientProperties planningModelClientProperties =
                new PlanningModelClientProperties();
        planningModelClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.OutboundUnitRestClientProperties outboundUnitRestClientProperties =
                new RestClientConfig.OutboundUnitRestClientProperties();
        outboundUnitRestClientProperties.setBaseUrl(BASE_URL);

        final LogisticCenterClientProperties logisticCenterClientProperties =
                new LogisticCenterClientProperties();
        logisticCenterClientProperties.setBaseUrl(BASE_URL);

        final AuthorizationClientProperties authorizationClientProperties =
                new AuthorizationClientProperties();
        authorizationClientProperties.setBaseUrl(BASE_URL);

        final AnalyticsClientProperties analyticsClientProperties =
                new AnalyticsClientProperties();
        analyticsClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.PlanningModelForecastClientProperties
                planningModelForecastClientProperties = new RestClientConfig
                .PlanningModelForecastClientProperties();
        planningModelForecastClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.OutboundWaveClientProperties outboundWaveRestClientProperties =
                new RestClientConfig.OutboundWaveClientProperties();
        outboundWaveRestClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.OutboundUnitSearchClientProperties unitSearchClientProperties =
                new RestClientConfig.OutboundUnitSearchClientProperties();
        unitSearchClientProperties.setBaseUrl(BASE_URL);

        return new RestClientConfig(
                planningModelClientProperties,
                outboundUnitRestClientProperties,
                logisticCenterClientProperties,
                authorizationClientProperties,
                analyticsClientProperties,
                planningModelForecastClientProperties,
                outboundWaveRestClientProperties,
                unitSearchClientProperties
        ).restClient();
    }

    public void cleanMocks() {
        RequestMockHolder.clear();
    }
}
