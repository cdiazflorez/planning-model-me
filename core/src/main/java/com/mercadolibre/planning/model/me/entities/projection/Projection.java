package com.mercadolibre.planning.model.me.entities.projection;

import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import lombok.Value;

import java.util.List;

@Value
public class Projection {

    private String title;

    /**
     * Projection/Deferrer date selector.
     */
    private DateSelector dateSelector;

    /** Message to show when the forecast is missing. */
    private String emptyStateMessage;

    private Data data;

    /** Tabs shown to the user. Los main ones are deferral and projection.
     * Note: This field is probably ignored by the front-end */
    private List<Tab> tabs;

    /**
     * Used only when the entering though the simulation button.
     */
    private SimulationMode simulationMode;

    public Projection(final String title,
                      final DateSelector dateSelector,
                      final Data data,
                      final List<Tab> tabs,
                      final SimulationMode simulationMode) {
        this.title = title;
        this.dateSelector = dateSelector;
        this.data = data;
        this.tabs = tabs;
        this.simulationMode = simulationMode;
        this.emptyStateMessage = null;
    }

    public Projection(final String title,
                      final DateSelector dateSelector,
                      final String emptyStateMessage,
                      final List<Tab> tabs,
                      final SimulationMode simulationMode) {
        this.title = title;
        this.dateSelector = dateSelector;
        this.data = null;
        this.tabs = tabs;
        this.simulationMode = simulationMode;
        this.emptyStateMessage = emptyStateMessage;
    }
}
