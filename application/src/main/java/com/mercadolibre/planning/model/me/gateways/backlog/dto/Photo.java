package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

/**
 * Class PhotoBacklog is used as currentPhotoBacklog and Photo in backlogHistorical.
 */
@Value
public class Photo {
  Instant takenOn;

  List<Group> groups;

  /**
   * Values per take_out.
   */
  @Value
  public static class Group {
    Map<BacklogGrouper, String> key;

    int total;

    public Optional<String> getGroupValue(final BacklogGrouper grouper) {
      return Optional.ofNullable(key.get(grouper));
    }
  }
}

