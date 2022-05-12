package com.mercadolibre.planning.model.me.gateways.toogle;

/** Feature toggles gateway. */
public interface FeatureSwitches {

    /**
     * Check enable calls to backlog api.
     *
     * @return whether the feature toggle is true.
     */
    boolean shouldCallBacklogApi();
}
