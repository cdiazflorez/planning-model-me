package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.util.List;

@Value
public class ComplexTable {

    private List<ColumnHeader> columns;

    private List<Data> data;
}

