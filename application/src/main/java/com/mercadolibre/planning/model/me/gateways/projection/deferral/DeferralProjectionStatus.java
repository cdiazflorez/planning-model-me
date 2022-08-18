package com.mercadolibre.planning.model.me.gateways.projection.deferral;

import java.time.Instant;
import lombok.Value;

@Value
public class DeferralProjectionStatus {

    Instant sla;

    Instant deferredAt;

    int deferredUnits;

    String deferralStatus;
}
