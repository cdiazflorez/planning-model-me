package com.mercadolibre.planning.model.me.controller.simulation.request;

import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SaveSimulationRequest {

    @NotNull
    private String warehouseId;

    @Valid
    @NotEmpty
    private List<SimulationRequest> simulations;

    @NotNull
    private ZonedDateTime date;
}
