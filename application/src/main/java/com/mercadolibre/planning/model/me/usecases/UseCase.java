package com.mercadolibre.planning.model.me.usecases;

@FunctionalInterface
public interface UseCase<T, P> {
    P execute(final T input);
}
