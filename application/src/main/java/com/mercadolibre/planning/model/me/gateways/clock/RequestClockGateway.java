package com.mercadolibre.planning.model.me.gateways.clock;

import java.time.Instant;

public interface RequestClockGateway {
    Instant now();
}
