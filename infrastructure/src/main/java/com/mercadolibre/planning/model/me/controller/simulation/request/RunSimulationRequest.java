package com.mercadolibre.planning.model.me.controller.simulation.request;

import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

@Builder
@Value
public class RunSimulationRequest {

    @NotNull
    private String warehouseId;

    @Valid
    @NotEmpty
    private List<SimulationRequest> simulations;
}
