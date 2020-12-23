package com.mercadolibre.planning.model.me.gateways.planningmodel.projection.backlog.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.utils.CustomDateZoneDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
public class ProjectionValue {

    @JsonDeserialize(using = CustomDateZoneDeserializer.class)
    ZonedDateTime date;

    int quantity;

    public static ProjectionValue fromEntity(final Entity entity) {
        return ProjectionValue.builder()
                .date(entity.getDate())
                .quantity(entity.getValue())
                .build();
    }

}
