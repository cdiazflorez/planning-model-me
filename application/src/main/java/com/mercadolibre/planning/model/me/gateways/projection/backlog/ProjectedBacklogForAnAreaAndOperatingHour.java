package com.mercadolibre.planning.model.me.gateways.projection.backlog;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import java.time.Instant;
import lombok.Value;

@Value
public class ProjectedBacklogForAnAreaAndOperatingHour {
  Instant operatingHour;

  ProcessName process;

  String area;

  BacklogProcessStatus status;

  Long quantity;

}
