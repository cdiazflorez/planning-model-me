package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.util.List;
import lombok.Value;

@Value
public class Simulation {
    private ProcessName processName;
    private List<SimulationEntity> entities;
}
