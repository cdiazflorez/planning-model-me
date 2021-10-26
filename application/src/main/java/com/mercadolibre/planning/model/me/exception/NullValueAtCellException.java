package com.mercadolibre.planning.model.me.exception;

import static java.lang.String.format;

@SuppressWarnings("AvoidEscapedUnicodeCharacters")
public class NullValueAtCellException extends RuntimeException {

    private static String MESSAGE = "El buffer (%s) no puede estar vac\u00edo, "
            + "y debe ser un n\u00famero v\u00e1lido";

    public NullValueAtCellException(String cell) {
        super(format(MESSAGE, cell));
    }

    public NullValueAtCellException(final Exception e) {
        super(MESSAGE, e);
    }
}
