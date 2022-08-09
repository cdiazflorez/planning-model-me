package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.util.List;
import lombok.Value;

@Value
public class BacklogProjectionResponse {

  ProcessName processName;

  List<ProjectionValue> values;
}
