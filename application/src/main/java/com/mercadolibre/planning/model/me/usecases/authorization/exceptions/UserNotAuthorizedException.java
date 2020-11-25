package com.mercadolibre.planning.model.me.usecases.authorization.exceptions;

public class UserNotAuthorizedException extends RuntimeException {
    public UserNotAuthorizedException(final String message) {
        super(message);
    }
}
