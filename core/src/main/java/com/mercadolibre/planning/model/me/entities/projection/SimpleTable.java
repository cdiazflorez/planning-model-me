package com.mercadolibre.planning.model.me.entities.projection;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class SimpleTable {

    String title;

    List<ColumnHeader> columns;

    List<Map<String,Object>> data;
}
