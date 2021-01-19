package com.mercadolibre.planning.model.me.entities.projection.complexTable;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import lombok.Value;

import java.util.List;

@Value
public class ComplexTable {

    private List<ColumnHeader> columns;

    private List<Data> data;

    private ComplexTableAction action;

    private String title;
}

