package com.mercadolibre.planning.model.me.controller.simulation.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
public class EntityRequest {

    @NotNull
    private String type;

    @Valid
    @NotEmpty
    private List<ValueRequest> values;

    public static SimulationEntity toSimulationEntity(final EntityRequest entity) {
        return new SimulationEntity(
                EntityType.from(entity.getType()),
                entity.getValues().stream().map(ValueRequest::toQuantityByDate).collect(toList())
        );
    }

}
