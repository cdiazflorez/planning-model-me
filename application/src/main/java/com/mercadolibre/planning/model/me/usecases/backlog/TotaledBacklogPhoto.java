package com.mercadolibre.planning.model.me.usecases.backlog;

import lombok.Value;

import java.time.Instant;

@Value
public class TotaledBacklogPhoto {
    Instant takenOn;
    Integer quantity;
}
