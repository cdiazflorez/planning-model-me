package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Fury configurations based feature toggle.
 */
@RefreshScope
@Component
public class FeatureToggle implements FeatureSwitches {

  @Value("${should-call-backlog-api}")
  private boolean callBacklogApi;

  @Value("${projection-lib-enabled-logistic-centers}")
  private Set<String> projectionLibEnabledLogisticCenters;

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
}
