package com.mercadolibre.planning.model.me.gateways.inboundreports.dto;

import java.util.List;
import lombok.Value;

@Value
public class InboundResponse {
  List<Aggregation> aggregations;

  @Value
  public static class Aggregation {
    String name;
    Integer value;
  }
}
