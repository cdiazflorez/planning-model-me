package com.mercadolibre.planning.model.me.entities.projection;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ColumnHeader {

    private String id;

    private String title;

    private String value;

    public ColumnHeader(final String id, final String title) {
        this.id = id;
        this.title = title;
        this.value = null;
    }

}
