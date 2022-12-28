package com.mercadolibre.planning.model.me.controller.deviation.request;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class DeviationRequest {

  @NotEmpty
  String warehouseId;
  @NotNull
  ZonedDateTime dateFrom;
  @NotNull
  ZonedDateTime dateTo;
  @NotNull
  Double value;
  @NotNull
  long userId;

  public SaveDeviationInput toDeviationInput(final Workflow workflow, final DeviationType type) {
    return SaveDeviationInput.builder()
        .workflow(workflow)
        .warehouseId(warehouseId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(value)
        .type(type)
        .userId(userId)
        .build();
  }
}
