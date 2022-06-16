package com.mercadolibre.planning.model.me.gateways.backlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    Map<String, String> key;

    int total;
  }
}

