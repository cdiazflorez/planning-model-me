package com.mercadolibre.planning.model.me.usecases.forecast.utils.excel;

import lombok.Value;

@Value
public class ErroneousCellValue<T> implements CellValue<T> {
    private final String error;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public T getValue() {
        throw new RuntimeException("no value");
    }
}
