package com.mercadolibre.planning.model.me.clients.rest.planningmodel.exception;

public class ForecastNotFoundException extends RuntimeException {

    public static final String MESSAGE = "No hay forecast cargado para la semana actual.";

    public ForecastNotFoundException() {
        super(MESSAGE);
    }
}
