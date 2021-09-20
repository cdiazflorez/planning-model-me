package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import lombok.Value;

import java.util.List;

@Value
public class Projection {

    private String title;

    private DateSelector dateSelector;

    private String emptyStateMessage;

    private Data data;

    List<Tab> tabs;

    SimulationMode simulationMode;
}
