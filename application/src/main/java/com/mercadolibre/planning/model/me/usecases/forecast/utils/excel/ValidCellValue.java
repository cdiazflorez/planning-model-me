package com.mercadolibre.planning.model.me.usecases.forecast.utils.excel;

import lombok.Value;

@Value
public class ValidCellValue<T> implements CellValue<T> {
    private final T value;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getError() {
        return "";
    }
}
