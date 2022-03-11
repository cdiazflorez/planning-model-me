package com.mercadolibre.planning.model.me.gateways.toogle;

/** Feature toggles gateway. */
public interface FeatureSwitches {

  /**
   * Check projection to to_pack feature toggle status.
   *
   * @param  logisticCenter target logistic center
   * @return                whether the feature toggle is active
   */
  boolean isProjectToPackEnabled(String logisticCenter);

}
