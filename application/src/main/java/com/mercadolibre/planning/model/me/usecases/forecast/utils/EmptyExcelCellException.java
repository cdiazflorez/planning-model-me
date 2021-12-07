package com.mercadolibre.planning.model.me.usecases.forecast.utils;

public class EmptyExcelCellException extends RuntimeException {

    public EmptyExcelCellException(String message) {
        super(message);
    }
}
