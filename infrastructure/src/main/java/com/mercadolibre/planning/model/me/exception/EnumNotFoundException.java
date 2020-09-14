package com.mercadolibre.planning.model.me.exception;

import lombok.Data;

import static java.lang.String.format;

@Data
public class EnumNotFoundException extends RuntimeException {

    public static final String MESSAGE_PATTERN = "Enum not found for value: %s";

    private String value;

    public EnumNotFoundException(final String value) {
        super();
        this.value = value;
    }

    public EnumNotFoundException(final String value, final Exception exception) {
        super(exception);
        this.value = value;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, value);
    }
}
