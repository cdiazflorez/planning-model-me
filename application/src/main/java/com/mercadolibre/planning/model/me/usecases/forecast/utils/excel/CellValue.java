package com.mercadolibre.planning.model.me.usecases.forecast.utils.excel;

public interface CellValue<T> {
    boolean isValid();

    String getError();

    T getValue();
}
