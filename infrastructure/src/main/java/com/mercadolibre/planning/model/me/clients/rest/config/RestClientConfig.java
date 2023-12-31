package com.mercadolibre.planning.model.me.clients.rest.config;

import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.AUTHORIZATION;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.BACKLOG;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.CONFIG;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.INBOUND_REPORTS;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.LOGISTIC_CENTER;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_SETTING;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_UNIT;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.OUTBOUND_UNIT_SEARCH;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL_FORECAST;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.PLANNING_MODEL_RATIOS;
import static com.mercadolibre.planning.model.me.clients.rest.config.RestPool.STAFFING;

import com.mercadolibre.restclient.MeliRESTPool;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.RESTPool;
import com.mercadolibre.restclient.cache.local.RESTLocalCache;
import com.mercadolibre.restclient.interceptor.AddTimeInterceptor;
import com.mercadolibre.restclient.retry.SimpleRetryStrategy;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({
    RestClientConfig.PlanningModelClientProperties.class,
    RestClientConfig.OutboundUnitRestClientProperties.class,
    RestClientConfig.LogisticCenterClientProperties.class,
    RestClientConfig.AuthorizationClientProperties.class,
    RestClientConfig.PlanningModelForecastClientProperties.class,
    RestClientConfig.PlanningModelMelysistemsClientProperties.class,
    RestClientConfig.OutboundWaveClientProperties.class,
    RestClientConfig.OutboundUnitSearchClientProperties.class,
    RestClientConfig.StaffingClientProperties.class,
    RestClientConfig.BacklogClientProperties.class,
    RestClientConfig.OutboundSettingsClientProperties.class,
    RestClientConfig.InboundReportsClientProperties.class,
    RestClientConfig.ConfigProperties.class
})
public class RestClientConfig {
  private static final int LOCAL_CACHE_LOGISTIC_CENTER = 30;
  private static final int LOCAL_CACHE_AUTHORIZATION = 200;
  private static final int LOCAL_CACHE_STAFFING = 100;
  private PlanningModelClientProperties planningModelClientProperties;
  private OutboundUnitRestClientProperties outboundUnitProperties;
  private LogisticCenterClientProperties logisticCenterClientProperties;
  private AuthorizationClientProperties authorizationClientProperties;
  private PlanningModelForecastClientProperties planningModelForecastClientProperties;
  private PlanningModelMelysistemsClientProperties planningModelMelysistemsClientProperties;
  private OutboundWaveClientProperties outboundWaveClientProperties;
  private OutboundUnitSearchClientProperties outboundUnitSearchClientProperties;
  private StaffingClientProperties staffingClientProperties;
  private BacklogClientProperties backlogClientProperties;
  private OutboundSettingsClientProperties outboundSettingsClientProperties;
  private InboundReportsClientProperties inboundReportsClientProperties;
  private ConfigProperties configProperties;

  @Bean
  public MeliRestClient restClient() throws IOException {
    return MeliRestClient
        .builder()
        .withPool(
            restPool(PLANNING_MODEL.name(), planningModelClientProperties),
            restPool(PLANNING_MODEL_RATIOS.name(), planningModelMelysistemsClientProperties),
            restPool(OUTBOUND_UNIT.name(), outboundUnitProperties),
            restPool(LOGISTIC_CENTER.name(),
                logisticCenterClientProperties, localCache(
                    "logistic_center", LOCAL_CACHE_LOGISTIC_CENTER)),
            restPool(AUTHORIZATION.name(),
                authorizationClientProperties, localCache(
                    "authorizations", LOCAL_CACHE_AUTHORIZATION)),
            restPool(PLANNING_MODEL_FORECAST.name(),
                planningModelForecastClientProperties),
            restPool(OUTBOUND_UNIT_SEARCH.name(),
                outboundUnitSearchClientProperties),
            restPool(STAFFING.name(), staffingClientProperties, localCache(
                "staffing", LOCAL_CACHE_STAFFING)),
            restPool(BACKLOG.name(), backlogClientProperties),
            restPool(OUTBOUND_SETTING.name(), outboundSettingsClientProperties),
            restPool(INBOUND_REPORTS.name(), inboundReportsClientProperties),
            restPool(CONFIG.name(), configProperties)
        )
        .build();
  }

  private RESTPool restPool(final String name, final RestClientProperties properties) {
    return restPool(name, properties, null);
  }

  private RESTPool restPool(final String name,
                            final RestClientProperties properties,
                            final RESTLocalCache cache) {

    return MeliRESTPool.builder()
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
  public static class OutboundUnitRestClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.logistic-center")
  public static class LogisticCenterClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.authorization")
  public static class AuthorizationClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.planning-model-forecast")
  public static class PlanningModelForecastClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.planning-model-melisystems")
  public static class PlanningModelMelysistemsClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.outbound-wave")
  public static class OutboundWaveClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.unit-search")
  public static class OutboundUnitSearchClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.staffing")
  public static class StaffingClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.backlog")
  public static class BacklogClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.outbound-settings")
  public static class OutboundSettingsClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.inbound-reports")
  public static class InboundReportsClientProperties extends RestClientProperties {
  }

  @ConfigurationProperties("restclient.pool.config-wms")
  public static class ConfigProperties extends RestClientProperties {
  }
}
