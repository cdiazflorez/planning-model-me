package com.mercadolibre.planning.model.me.gateways.projection.backlog;

import java.time.Instant;
import lombok.Value;

@Value
public class BacklogQuantity {

    Instant date;

    int quantity;
}
