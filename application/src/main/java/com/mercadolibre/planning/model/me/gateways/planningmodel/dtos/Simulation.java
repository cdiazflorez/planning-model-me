package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Simulation {
    private ProcessName processName;
    private List<SimulationEntity> entities;
}
