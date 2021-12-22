package com.mercadolibre.planning.model.me.usecases.backlog;

import java.time.Instant;
import lombok.Value;

@Value
public class TotaledBacklogPhoto {
    Instant takenOn;
    Integer quantity;
}
