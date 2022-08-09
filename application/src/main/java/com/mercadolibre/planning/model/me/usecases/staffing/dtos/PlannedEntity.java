package com.mercadolibre.planning.model.me.usecases.staffing.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlannedEntity {
  private MagnitudeType type;
  private ZonedDateTime date;
  private Workflow workflow;
  private ProcessName processName;
  private Integer value;

  public static PlannedEntity fromEntity(
      final MagnitudePhoto magVarPhoto,
      final MagnitudeType type
  ) {
    return PlannedEntity.builder()
        .type(type)
        .date(magVarPhoto.getDate())
        .workflow(magVarPhoto.getWorkflow())
        .processName(magVarPhoto.getProcessName())
        .value(magVarPhoto.getValue())
        .build();
  }
}
