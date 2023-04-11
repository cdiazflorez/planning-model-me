package com.mercadolibre.planning.model.me.clients.rest;

import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.AuthorizationClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.LogisticCenterClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.PlanningModelClientProperties;
import com.mercadolibre.planning.model.me.clients.rest.config.RestClientConfig.PlanningModelMelysistemsClientProperties;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.mock.RequestMockHolder;

import java.io.IOException;

public class BaseClientTest {

    protected static final String BASE_URL = "http://internal.mercadolibre.com";
    protected static final String BASE_URL_MELISYSTEMS = "http://fbm-flow.melisystems.com";

    protected MeliRestClient getRestTestClient() throws IOException {

        final PlanningModelClientProperties planningModelClientProperties =
                new PlanningModelClientProperties();
        planningModelClientProperties.setBaseUrl(BASE_URL);

        final PlanningModelMelysistemsClientProperties melysistemsClientProperties =
            new PlanningModelMelysistemsClientProperties();
        melysistemsClientProperties.setBaseUrl(BASE_URL_MELISYSTEMS);

        final RestClientConfig.OutboundUnitRestClientProperties outboundUnitRestClientProperties =
                new RestClientConfig.OutboundUnitRestClientProperties();
        outboundUnitRestClientProperties.setBaseUrl(BASE_URL);

        final LogisticCenterClientProperties logisticCenterClientProperties =
                new LogisticCenterClientProperties();
        logisticCenterClientProperties.setBaseUrl(BASE_URL);

        final AuthorizationClientProperties authorizationClientProperties =
                new AuthorizationClientProperties();
        authorizationClientProperties.setBaseUrl(BASE_URL);

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

        final RestClientConfig.StaffingClientProperties staffingClientProperties =
                new RestClientConfig.StaffingClientProperties();
        staffingClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.BacklogClientProperties backlogClientProperties =
                new RestClientConfig.BacklogClientProperties();
        backlogClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.OutboundSettingsClientProperties outboundSettingsClientProperties =
                new RestClientConfig.OutboundSettingsClientProperties();
        outboundSettingsClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.InboundReportsClientProperties inboundReportsClientProperties =
            new RestClientConfig.InboundReportsClientProperties();
        inboundReportsClientProperties.setBaseUrl(BASE_URL);

        final RestClientConfig.ConfigProperties configProperties = new RestClientConfig.ConfigProperties();
        configProperties.setBaseUrl(BASE_URL);

        return new RestClientConfig(
                planningModelClientProperties,
                outboundUnitRestClientProperties,
                logisticCenterClientProperties,
                authorizationClientProperties,
                planningModelForecastClientProperties,
                melysistemsClientProperties,
                outboundWaveRestClientProperties,
                unitSearchClientProperties,
                staffingClientProperties,
                backlogClientProperties,
                outboundSettingsClientProperties,
                inboundReportsClientProperties,
                configProperties

        ).restClient();
    }

    public void cleanMocks() {
        RequestMockHolder.clear();
    }
}
