package com.mercadolibre.planning.model.me.exception;

import static java.lang.String.format;

public class NullValueAtCellException extends RuntimeException {

    public static final String MESSAGE = "El buffer (%s) no puede estar vacío, "
            + "y debe ser un número válido";

    public NullValueAtCellException(String cell) {
        super(format(MESSAGE, cell));
    }

    public NullValueAtCellException(final Exception e) {
        super(MESSAGE, e);
    }
}
