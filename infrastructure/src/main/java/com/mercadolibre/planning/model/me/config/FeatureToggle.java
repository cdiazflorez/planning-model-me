package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.usecases.forecast.UploadForecast;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Fury configurations based feature toggle.
 */
@RefreshScope
@Component
public class FeatureToggle implements FeatureSwitches, UploadForecast.FeatureToggles {

  @Value("${should-call-backlog-api}")
  private boolean callBacklogApi;

  @Value("${projection-lib-enabled-logistic-centers}")
  private Set<String> projectionLibEnabledLogisticCenters;

  @Value("${should-read-check-in-tph}")
  private Set<String> checkInTphEnabledByLogisticCenter;

  /**
   * Reads configuration for enable calls to backlog api.
   *
   * @return whether the calls to backlog api is enabled.
   */
  @Override
  public boolean shouldCallBacklogApi() {
    return callBacklogApi;
  }

  /**
   * Reads configuration projection lib.
   *
   * @param logisticCenter target logistic center
   * @return logistics centers enabled
   */
  @Override
  public boolean isProjectionLibEnabled(final String logisticCenter) {
    return projectionLibEnabledLogisticCenters.contains(logisticCenter);
  }

  /**
   * Check if Read CheckIn TPH is enabled on staffing planning by logisticCenter.
   *
   * @param logisticCenter target logistic center
   * @return whether the feature toggle is active
   */
  @Override
  public boolean isReadCheckInTphEnabled(final String logisticCenter) {
    return checkInTphEnabledByLogisticCenter.contains(logisticCenter);
  }
}
