package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import lombok.Value;

import java.util.List;

@Value
public class Projection {

    private String title;

    /** Projection/Deferrer date selector*/
    private DateSelector dateSelector;

    /** Mensaje cuando no hay forecast */
    private String emptyStateMessage;

    /** Todo el payload */
    private Data data;

    /** Tabs que se le muestran al usuario. Los principale son diferimiento y proyección */
    List<Tab> tabs;

    /** no se usa para GetProjection. Solo se usa pasando por botón simulation */
    SimulationMode simulationMode;
}
