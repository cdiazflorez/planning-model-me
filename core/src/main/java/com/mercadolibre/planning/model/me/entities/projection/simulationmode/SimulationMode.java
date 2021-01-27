package com.mercadolibre.planning.model.me.entities.projection.simulationmode;

import lombok.Value;

@Value
public class SimulationMode {

    String startLabel;

    Snackbar snackbar;

    ErrorMessage message;
}
