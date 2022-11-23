package com.mercadolibre.planning.model.me.usecases.forecast.utils;

import static java.lang.String.format;

public class ProcessPathNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Process path not found for value: %s";
    private static final long serialVersionUID = -5176239136017481827L;

    private final String value;

    public ProcessPathNotFoundException(final String value, final Exception exception) {
        super(exception);
        this.value = value;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, value);
    }
}
