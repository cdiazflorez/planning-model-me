package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import lombok.Value;

import java.util.List;

@Value
public class Simulation {
    private ProcessName processName;
    private List<SimulationEntity> entities;
}
