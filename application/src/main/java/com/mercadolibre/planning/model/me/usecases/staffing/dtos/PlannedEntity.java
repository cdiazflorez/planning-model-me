package com.mercadolibre.planning.model.me.usecases.staffing.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class PlannedEntity {
    private EntityType type;
    private ZonedDateTime date;
    private Workflow workflow;
    private ProcessName processName;
    private Integer value;

    public static PlannedEntity fromEntity(final Entity entity, final EntityType type) {
        return PlannedEntity.builder()
                .type(type)
                .date(entity.getDate())
                .workflow(entity.getWorkflow())
                .processName(entity.getProcessName())
                .value(entity.getValue())
                .build();
    }
}
