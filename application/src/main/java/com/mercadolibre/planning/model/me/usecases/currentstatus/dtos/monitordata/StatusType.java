package com.mercadolibre.planning.model.me.usecases.currentstatus.dtos.monitordata;

public enum StatusType {
    PENDING,
    TO_PICK,
    TO_PACK,
    TO_GROUP;

    public String toName() {
        return this.name().toLowerCase();
    }
}
