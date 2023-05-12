package com.mercadolibre.planning.model.me.controller.simulation.request;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class EntityRequest {

    @NotNull
    private String type;

    @Valid
    @NotEmpty
    private List<ValueRequest> values;

    public static SimulationEntity toSimulationEntity(final EntityRequest entity) {
        return new SimulationEntity(
                MagnitudeType.from(entity.getType()),
                entity.getValues().stream().map(ValueRequest::toQuantityByDate).collect(toList())
        );
    }

}
