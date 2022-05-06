package com.mercadolibre.planning.model.me.gateways.staffing.dtos.response;

import java.util.List;
import lombok.Value;

@Value
public class ProcessResponse {

  String name;
  Double value;
  List<AreaResponse> areas;
}
