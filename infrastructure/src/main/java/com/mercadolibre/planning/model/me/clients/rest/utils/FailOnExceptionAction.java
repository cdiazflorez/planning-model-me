package com.mercadolibre.planning.model.me.clients.rest.utils;

import com.mercadolibre.resilience.breaker.Action;

public interface FailOnExceptionAction<T> extends Action<T> {

    @Override
    default boolean isValid(T result, Throwable throwable) {
        return result != null || throwable != null;
    }
}
