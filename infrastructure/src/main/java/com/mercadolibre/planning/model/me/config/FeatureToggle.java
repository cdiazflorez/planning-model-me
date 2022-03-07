package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** Fury configurations based feature toggle. */
@RefreshScope
@Component
public class FeatureToggle implements FeatureSwitches {

  @Value("${project-to-pack.enabled-logistic-centers}")
  private Set<String> projectionToPackEnabledLogisticCenters;

  /**
   * Reads configuration for to to_pack sla projection.
   *
   * @param  logisticCenter target logistic center
   * @return                whether the projection to to_pack is enabled for the selected logistic center
   */
  @Override
  public boolean isProjectToPackEnabled(final String logisticCenter) {
    return projectionToPackEnabledLogisticCenters.contains(logisticCenter);
  }
}
