package com.mercadolibre.planning.model.me.usecases.forecast.parsers;

/**
 * Validator.
 * It is validator generic for outbound or inbound.
 *
 * @param <T> is a generic type for distinct validations
 */
public interface Validator<T> {
  void validate(T input);
}
