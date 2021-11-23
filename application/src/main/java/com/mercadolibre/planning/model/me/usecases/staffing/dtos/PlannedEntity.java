package com.mercadolibre.planning.model.me.usecases.staffing.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagVarPhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class PlannedEntity {
    private MagnitudeType type;
    private ZonedDateTime date;
    private Workflow workflow;
    private ProcessName processName;
    private Integer value;

    public static PlannedEntity fromEntity(
            final MagVarPhoto magVarPhoto,
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
