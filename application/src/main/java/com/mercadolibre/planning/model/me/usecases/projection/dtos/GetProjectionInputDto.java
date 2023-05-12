package com.mercadolibre.planning.model.me.usecases.projection.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetProjectionInputDto {

    private Workflow workflow;

    private String warehouseId;

    private List<Simulation> simulations;

    private long userId;

    private ZonedDateTime date;

    private Instant requestDate;
}
