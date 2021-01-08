package com.mercadolibre.planning.model.me.clients.rest.config;

import com.mercadolibre.restclient.RESTPool;
import com.mercadolibre.restclient.RestClient;
import com.mercadolibre.restclient.cache.local.RESTLocalCache;
import com.mercadolibre.restclient.interceptor.AddTimeInterceptor;
import com.mercadolibre.restclient.retry.SimpleRetryStrategy;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.ANALYTICS;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.AUTHORIZATION;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.LOGISTIC_CENTER;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_UNIT;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_WAVE;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL_FORECAST;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({
        RestClientConfig.PlanningModelClientProperties.class,
        RestClientConfig.OutboundUnitRestClientProperties.class,
        RestClientConfig.LogisticCenterClientProperties.class,
        RestClientConfig.AuthorizationClientProperties.class,
        RestClientConfig.AnalyticsClientProperties.class,
        RestClientConfig.PlanningModelForecastClientProperties.class,
        RestClientConfig.OutboundWaveClientProperties.class
})
public class RestClientConfig {
    private PlanningModelClientProperties planningModelClientProperties;
    private OutboundUnitRestClientProperties outboundUnitProperties;
    private LogisticCenterClientProperties logisticCenterClientProperties;
    private AuthorizationClientProperties authorizationClientProperties;
    private AnalyticsClientProperties analyticsClientProperties;
    private PlanningModelForecastClientProperties planningModelForecastClientProperties;
    private OutboundWaveClientProperties outboundWaveClientProperties;

    @Bean
    public RestClient restClient() throws IOException {
        return RestClient
                .builder()
                .withPool(
                        restPool(PLANNING_MODEL.name(), planningModelClientProperties),
                        restPool(OUTBOUND_UNIT.name(), outboundUnitProperties),
                        restPool(LOGISTIC_CENTER.name(),
                                logisticCenterClientProperties, localCache(
                                        "logistic_center", 30)),
                        restPool(AUTHORIZATION.name(),
                                authorizationClientProperties, localCache(
                                        "authorizations", 200)),
                        restPool(ANALYTICS.name(),
                                analyticsClientProperties, localCache(
                                        "analytics", 200)),
                        restPool(PLANNING_MODEL_FORECAST.name(),
                                planningModelForecastClientProperties),
                        restPool(OUTBOUND_WAVE.name(),
                                outboundWaveClientProperties)
                )
                .build();
    }

    private RESTPool restPool(final String name, final RestClientProperties properties) {
        return restPool(name, properties, null);
    }

    private RESTPool restPool(final String name,
                              final RestClientProperties properties,
                              final RESTLocalCache cache) {

        return RESTPool.builder()
                .withName(name)
                .withBaseURL(properties.getBaseUrl())
                .withConnectionTimeout(properties.getConnectionTimeout())
                .withMaxIdleTime(properties.getMaxIdleTime())
                .withMaxPoolWait(properties.getMaxPoolWait())
                .withRetryStrategy(new SimpleRetryStrategy(
                        properties.getMaxRetries(),
                        properties.getRetriesDelay()))
                .withSocketTimeout(properties.getSocketTimeout())
                .withValidationOnInactivity(properties.getValidationOnInactivity())
                .withWorkerThreads(properties.getWorkerThreads())
                .addInterceptorLast(RestClientLoggingInterceptor.INSTANCE)
                .addInterceptorLast(AddTimeInterceptor.INSTANCE)
                .withCache(cache)
                .build();
    }

    private RESTLocalCache localCache(final String name, final int elements) {
        return new RESTLocalCache(name, elements);
    }

    @ConfigurationProperties("restclient.pool.planning-model")
    public static class PlanningModelClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.outbound-unit")
    public static class OutboundUnitRestClientProperties extends RestClientProperties {}

    @ConfigurationProperties("restclient.pool.logistic-center")
    public static class LogisticCenterClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.authorization")
    public static class AuthorizationClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.analytics")
    public static class AnalyticsClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.planning-model-forecast")
    public static class PlanningModelForecastClientProperties extends RestClientProperties {
    }

    @ConfigurationProperties("restclient.pool.outbound-wave")
    public static class OutboundWaveClientProperties extends RestClientProperties {
    }
}
