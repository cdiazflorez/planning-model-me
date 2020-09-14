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

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_UNIT;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({
        RestClientConfig.PlanningModelClientProperties.class,
        RestClientConfig.OutboundUnitRestClientProperties.class
})
public class RestClientConfig {

    private PlanningModelClientProperties planningModelClientProperties;
    private OutboundUnitRestClientProperties outboundUnitProperties;

    @Bean
    public RestClient restClient() throws IOException {
        return RestClient
                .builder()
                .withPool(
                        restPool(PLANNING_MODEL.name(), planningModelClientProperties),
                        restPool(OUTBOUND_UNIT.name(), outboundUnitProperties)
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
}
