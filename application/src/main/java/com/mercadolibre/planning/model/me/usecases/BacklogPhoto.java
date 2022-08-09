package com.mercadolibre.planning.model.me.usecases;

import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

@Value
public class BacklogPhoto {
  Instant takenOn;
  Integer quantity;
  Map<BacklogGrouper, String> groups;

  public BacklogPhoto(final Instant takenOn, final Integer quantity) {
    this.takenOn = takenOn;
    this.quantity = quantity;
    this.groups = Collections.emptyMap();
  }

  public Optional<String> getGroupValue(final BacklogGrouper grouper) {
    return Optional.ofNullable(groups.get(grouper));
  }
}
