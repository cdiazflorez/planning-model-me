package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class SimpleTable {
    private String title;
    private List<ColumnHeader> columns;
    private List<Map<String,Object>> data;
}
