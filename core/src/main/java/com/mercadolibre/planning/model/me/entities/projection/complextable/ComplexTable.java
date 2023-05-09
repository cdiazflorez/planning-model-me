package com.mercadolibre.planning.model.me.entities.projection.complextable;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import java.util.List;
import lombok.Value;

@Value
public class ComplexTable {

    private List<ColumnHeader> columns;

    private List<Data> data;

    private ComplexTableAction action;

    private String title;
}

