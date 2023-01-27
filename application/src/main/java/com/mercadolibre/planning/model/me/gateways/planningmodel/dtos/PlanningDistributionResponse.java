package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningDistributionResponse {
  @JsonDeserialize(using = CustomDateZoneDeserializer.class)
  private ZonedDateTime dateIn;

  @JsonDeserialize(using = CustomDateZoneDeserializer.class)
  private ZonedDateTime dateOut;

  private MetricUnit metricUnit;

  private long total;

  private boolean isDeferred;
}
