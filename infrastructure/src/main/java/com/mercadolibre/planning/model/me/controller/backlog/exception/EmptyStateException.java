package com.mercadolibre.planning.model.me.controller.backlog.exception;

public class EmptyStateException extends RuntimeException {
    public static final String MESSAGE = "Por el momento no tenemos información para mostrar.";

    public EmptyStateException() {
        super(MESSAGE);
    }
}
