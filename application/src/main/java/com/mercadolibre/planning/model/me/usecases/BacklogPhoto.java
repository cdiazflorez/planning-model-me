package com.mercadolibre.planning.model.me.usecases;

import java.time.Instant;
import lombok.Value;

@Value
public class BacklogPhoto {
  Instant takenOn;
  Integer quantity;
}
