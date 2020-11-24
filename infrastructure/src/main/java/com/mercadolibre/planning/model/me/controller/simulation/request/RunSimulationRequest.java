package com.mercadolibre.planning.model.me.controller.simulation.request;

import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

@Value
public class RunSimulationRequest {

    @NotNull
    private String warehouseId;

    @Valid
    @NotEmpty
    private List<SimulationRequest> simulations;
}