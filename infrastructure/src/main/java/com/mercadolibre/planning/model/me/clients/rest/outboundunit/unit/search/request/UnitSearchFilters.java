package com.mercadolibre.planning.model.me.clients.rest.outboundunit.unit.search.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UnitSearchFilters {

    GROUP_WAREHOUSE_ID("group.warehouse_id"),
    GROUP_ETD_FROM("group.etd_from"),
    GROUP_TYPE("group.type"),
    GROUP_ETD_TO("group.etd_to"),
    GROUP_DATE_CREATED_FROM("group.date_created_from"),
    GROUP_DATE_CREATED_TO("group.date_created_to");

    private String fieldName;

    UnitSearchFilters(final String fieldName) {
        this.fieldName = fieldName;
    }

    @JsonValue
    public String getFieldName() {
        return fieldName;
    }


}
