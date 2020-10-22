package com.mercadolibre.planning.model.me.exception;

import static java.lang.String.format;

public class UnmatchedWarehouseException extends RuntimeException {

    private final String paramWarehouse;
    private final String excelWarehouse;

    public UnmatchedWarehouseException(final String paramWarehouse, final String excelWarehouse) {
        super();
        this.paramWarehouse = paramWarehouse;
        this.excelWarehouse = excelWarehouse;
    }

    public static final String MESSAGE_PATTERN =
            "Warehouse id %s is different from warehouse id %s from file";

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, paramWarehouse, excelWarehouse);
    }
}
