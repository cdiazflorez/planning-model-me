package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.gateways.clock.RequestClockGateway;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * A frozen clock that is created for each request.
 */
@Component
@RequestScope
public class RequestClock implements RequestClockGateway {
    private final Instant requestInstant;

    public RequestClock() {
        requestInstant = Instant.now();
    }

    /**
     * Gives the instant when the request associated to this thread was received.
     * @return the instant when the request was received.
     */
    public Instant now() {
        return requestInstant;
    }
}
