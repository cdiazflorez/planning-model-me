package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** Fury configurations based feature toggle. */
@RefreshScope
@Component
public class FeatureToggle implements FeatureSwitches {

}
