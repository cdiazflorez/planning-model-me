package com.mercadolibre.planning.model.me.exception;

public class ForecastParsingException extends RuntimeException {

    public ForecastParsingException(final String message) {
        super(message);
    }

    public ForecastParsingException(final String message, final Exception e) {
        super(message, e);
    }
}
