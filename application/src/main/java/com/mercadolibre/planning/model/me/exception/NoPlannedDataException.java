package com.mercadolibre.planning.model.me.exception;

public class NoPlannedDataException extends RuntimeException {

    public static final String MESSAGE = "There is no data because forecast is not found";

    public NoPlannedDataException() {
        super(MESSAGE);
    }

    public NoPlannedDataException(final Exception e) {
        super(MESSAGE, e);
    }
}
