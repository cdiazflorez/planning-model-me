package com.mercadolibre.planning.model.me.controller.simulation.request;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
public class SimulationRequest {

    @NotNull
    private String processName;

    @Valid
    @NotEmpty
    private List<EntityRequest> entities;

    public static Simulation toSimulation(final SimulationRequest simulation) {
        return new Simulation(
                ProcessName.from(simulation.getProcessName()),
                simulation.getEntities().stream()
                        .map(EntityRequest::toSimulationEntity)
                        .collect(toList())
        );
    }
}
