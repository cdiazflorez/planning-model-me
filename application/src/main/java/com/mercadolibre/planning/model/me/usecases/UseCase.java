package com.mercadolibre.planning.model.me.usecases;

public interface UseCase<T, P> {
    P execute(final T input);
}
