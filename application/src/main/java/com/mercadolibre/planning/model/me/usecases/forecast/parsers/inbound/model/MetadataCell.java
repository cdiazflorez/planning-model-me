package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MetadataCell {
    WAREHOUSE_ID(3, 2),
    WEEK(2, 2);

    private final int row;
    private final int column;
}
