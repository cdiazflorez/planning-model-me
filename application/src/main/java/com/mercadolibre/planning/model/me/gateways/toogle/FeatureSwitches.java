package com.mercadolibre.planning.model.me.gateways.toogle;

/**
 * Feature toggles gateway.
 */
public interface FeatureSwitches {

  /**
   * Check enable calls to backlog api.
   *
   * @return whether the feature toggle is true.
   */
  boolean shouldCallBacklogApi();

  /**
   * Check projection lib feature toggle status.
   *
   * @param logisticCenter target logistic center
   * @return whether the feature toggle is active
   */
  boolean isProjectionLibEnabled(String logisticCenter);
}
