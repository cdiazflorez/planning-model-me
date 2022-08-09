package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import lombok.Value;

import java.util.List;

@Value
public class Simulation {
    private ProcessName processName;
    private List<SimulationEntity> entities;
}
